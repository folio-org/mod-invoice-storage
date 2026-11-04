package org.folio.service.calendar;

import static org.folio.service.calendar.CalendarTriggerService.INVOICE_ENTITY_TYPE;
import static org.folio.service.calendar.CalendarTriggerService.INVOICE_PAYMENT_DUE;
import static org.folio.service.calendar.CalendarTriggerService.MODULE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.rest.jaxrs.model.CalendarMatch;
import org.folio.rest.jaxrs.model.CalendarMatchEvent;
import org.folio.rest.jaxrs.model.CalendarTriggerEvent;
import org.folio.rest.jaxrs.model.CalendarTriggerRule;
import org.folio.rest.persist.DBClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.vertx.core.Future;

class CalendarTriggerServiceTest {

  private static final int CHUNK_SIZE = 2;
  private static final String FIRST_ID = "00000000-0000-0000-0000-000000000000";
  private static final List<String> TARGET_DATES = List.of("2026-11-02", "2026-11-09");

  private final InvoiceDAO invoiceDAO = mock(InvoiceDAO.class);
  private final CalendarMatchProducer producer = mock(CalendarMatchProducer.class);
  private final DBClient client = mock(DBClient.class);
  private final Map<String, String> okapiHeaders = Map.of("x-okapi-tenant", "diku");
  private final CalendarTriggerService service = new CalendarTriggerService(invoiceDAO, producer, CHUNK_SIZE);

  @Test
  void publishesOneMatchEventPerPage() {
    var first = match("2026-11-02");
    var second = match("2026-11-09");
    var third = match("2026-11-09");
    when(invoiceDAO.getUnpaidInvoicesByPaymentDue(TARGET_DATES, FIRST_ID, CHUNK_SIZE, client))
      .thenReturn(Future.succeededFuture(List.of(first, second)));
    when(invoiceDAO.getUnpaidInvoicesByPaymentDue(TARGET_DATES, second.getRecordId(), CHUNK_SIZE, client))
      .thenReturn(Future.succeededFuture(List.of(third)));
    when(producer.sendMatchEvent(any(), eq(okapiHeaders))).thenReturn(Future.succeededFuture());
    var event = triggerEvent(rule(INVOICE_PAYMENT_DUE, TARGET_DATES));

    var result = service.processTrigger(event, client, okapiHeaders);

    assertEquals(3, result.result());
    var captor = ArgumentCaptor.forClass(CalendarMatchEvent.class);
    verify(producer, times(2)).sendMatchEvent(captor.capture(), eq(okapiHeaders));
    var sent = captor.getAllValues();
    assertEquals(List.of(first, second), sent.get(0).getMatches());
    assertEquals(List.of(third), sent.get(1).getMatches());
    sent.forEach(matchEvent -> {
      assertEquals(event.getId(), matchEvent.getTriggerEventId());
      assertEquals("diku", matchEvent.getTenant());
      assertEquals(MODULE_NAME, matchEvent.getSourceModule());
      assertEquals(event.getBusinessDate(), matchEvent.getBusinessDate());
      matchEvent.getMatches().forEach(match -> {
        assertEquals(INVOICE_PAYMENT_DUE, match.getIntent());
        assertEquals(INVOICE_ENTITY_TYPE, match.getEntityType());
      });
    });
  }

  @Test
  void stopsAtEmptyPage_whenLastPageIsFull() {
    var first = match("2026-11-02");
    var second = match("2026-11-02");
    when(invoiceDAO.getUnpaidInvoicesByPaymentDue(TARGET_DATES, FIRST_ID, CHUNK_SIZE, client))
      .thenReturn(Future.succeededFuture(List.of(first, second)));
    when(invoiceDAO.getUnpaidInvoicesByPaymentDue(TARGET_DATES, second.getRecordId(), CHUNK_SIZE, client))
      .thenReturn(Future.succeededFuture(List.of()));
    when(producer.sendMatchEvent(any(), eq(okapiHeaders))).thenReturn(Future.succeededFuture());

    var result = service.processTrigger(triggerEvent(rule(INVOICE_PAYMENT_DUE, TARGET_DATES)), client, okapiHeaders);

    assertEquals(2, result.result());
    verify(producer, times(1)).sendMatchEvent(any(), any());
  }

  @Test
  void skipsUnsupportedIntentAndRuleWithoutDates() {
    var event = triggerEvent(rule("OrderReviewDue", TARGET_DATES), rule(INVOICE_PAYMENT_DUE, List.of()));

    var result = service.processTrigger(event, client, okapiHeaders);

    assertEquals(0, result.result());
    verify(invoiceDAO, never()).getUnpaidInvoicesByPaymentDue(any(), anyString(), anyInt(), any());
    verify(producer, never()).sendMatchEvent(any(), any());
  }

  @Test
  void failsAndStops_whenMatchEventIsNotSent() {
    var first = match("2026-11-02");
    var second = match("2026-11-02");
    when(invoiceDAO.getUnpaidInvoicesByPaymentDue(TARGET_DATES, FIRST_ID, CHUNK_SIZE, client))
      .thenReturn(Future.succeededFuture(List.of(first, second)));
    when(producer.sendMatchEvent(any(), eq(okapiHeaders)))
      .thenReturn(Future.failedFuture(new IllegalStateException("broker down")));

    var result = service.processTrigger(triggerEvent(rule(INVOICE_PAYMENT_DUE, TARGET_DATES)), client, okapiHeaders);

    assertTrue(result.failed());
    verify(invoiceDAO, times(1)).getUnpaidInvoicesByPaymentDue(any(), anyString(), anyInt(), any());
  }

  private static CalendarTriggerEvent triggerEvent(CalendarTriggerRule... rules) {
    return new CalendarTriggerEvent()
      .withId(UUID.randomUUID().toString())
      .withTenant("diku")
      .withTargetModule(MODULE_NAME)
      .withBusinessDate("2026-11-02")
      .withRules(List.of(rules));
  }

  private static CalendarTriggerRule rule(String intent, List<String> targetDates) {
    return new CalendarTriggerRule().withIntent(intent).withTargetDates(targetDates);
  }

  private static CalendarMatch match(String paymentDue) {
    return new CalendarMatch().withRecordId(UUID.randomUUID().toString()).withTargetDate(paymentDue);
  }

}
