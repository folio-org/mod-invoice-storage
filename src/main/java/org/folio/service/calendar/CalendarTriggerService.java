package org.folio.service.calendar;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.rest.jaxrs.model.CalendarMatch;
import org.folio.rest.jaxrs.model.CalendarMatchEvent;
import org.folio.rest.jaxrs.model.CalendarTriggerEvent;
import org.folio.rest.jaxrs.model.CalendarTriggerRule;
import org.folio.rest.persist.DBClient;

import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Finds the invoices a calendar trigger event from mod-notification-center asks for and publishes them as calendar
 * match events, one event per page of matches.
 */
@Log4j2
@RequiredArgsConstructor
public class CalendarTriggerService {

  public static final String MODULE_NAME = "mod-invoice-storage";
  static final String INVOICE_PAYMENT_DUE = "InvoicePaymentDue";
  static final String INVOICE_ENTITY_TYPE = "invoice";
  private static final String FIRST_ID = "00000000-0000-0000-0000-000000000000";

  private final InvoiceDAO invoiceDAO;
  private final CalendarMatchProducer producer;
  private final int chunkSize;

  /**
   * Publishes the matches of every rule of the event, one rule after another.
   *
   * @param event        the calendar trigger event
   * @param client       the db client of the event's tenant
   * @param okapiHeaders the okapi headers of the event
   * @return future with the number of published matches, failed when a query or a send failed
   */
  public Future<Integer> processTrigger(CalendarTriggerEvent event, DBClient client, Map<String, String> okapiHeaders) {
    log.info("processTrigger:: Processing trigger event with id: '{}', businessDate: '{}', rules: {}",
      event.getId(), event.getBusinessDate(), event.getRules().size());
    Future<Integer> result = Future.succeededFuture(0);
    for (var rule : event.getRules()) {
      result = result.compose(total -> processRule(event, rule, client, okapiHeaders).map(count -> total + count));
    }
    return result
      .onSuccess(total -> log.info("processTrigger:: Trigger event with id: '{}' processed, {} matches published",
        event.getId(), total))
      .onFailure(t -> log.error("processTrigger:: Failed to process trigger event with id: '{}'", event.getId(), t));
  }

  private Future<Integer> processRule(CalendarTriggerEvent event, CalendarTriggerRule rule, DBClient client,
                                      Map<String, String> okapiHeaders) {
    if (!INVOICE_PAYMENT_DUE.equals(rule.getIntent())) {
      log.warn("processRule:: Intent '{}' of trigger event with id: '{}' is not supported, skipping it",
        rule.getIntent(), event.getId());
      return Future.succeededFuture(0);
    }
    if (rule.getTargetDates().isEmpty()) {
      return Future.succeededFuture(0);
    }
    return publishPages(event, rule, FIRST_ID, 0, client, okapiHeaders);
  }

  private Future<Integer> publishPages(CalendarTriggerEvent event, CalendarTriggerRule rule, String afterId,
                                       int published, DBClient client, Map<String, String> okapiHeaders) {
    return invoiceDAO.getInvoicesByPaymentDue(rule.getTargetDates(), afterId, chunkSize, client)
      .compose(page -> {
        if (page.isEmpty()) {
          return Future.succeededFuture(published);
        }
        page.forEach(match -> match.withIntent(rule.getIntent()).withEntityType(INVOICE_ENTITY_TYPE));
        return producer.sendMatchEvent(matchEvent(event, page), okapiHeaders)
          .compose(v -> page.size() < chunkSize
            ? Future.succeededFuture(published + page.size())
            : publishPages(event, rule, page.getLast().getRecordId(), published + page.size(), client, okapiHeaders));
      });
  }

  private CalendarMatchEvent matchEvent(CalendarTriggerEvent event, List<CalendarMatch> matches) {
    return new CalendarMatchEvent()
      .withId(UUID.randomUUID().toString())
      .withTriggerEventId(event.getId())
      .withTenant(event.getTenant())
      .withSourceModule(MODULE_NAME)
      .withBusinessDate(event.getBusinessDate())
      .withMatches(matches);
  }

}
