package org.folio.rest.impl;

import static org.folio.rest.impl.StorageTestSuite.KAFKA_ENV_VALUE;
import static org.folio.rest.impl.StorageTestSuite.checkKafkaEventSent;
import static org.folio.rest.impl.StorageTestSuite.sendKafkaRecord;
import static org.folio.service.calendar.CalendarMatchProducer.CALENDAR_MATCH_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.CalendarMatch;
import org.folio.rest.jaxrs.model.CalendarMatchEvent;
import org.folio.rest.jaxrs.model.CalendarTriggerEvent;
import org.folio.rest.jaxrs.model.CalendarTriggerRule;
import org.folio.rest.utils.TestData;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class CalendarTriggerConsumerTest extends TestBase {

  private static final String TRIGGER_TOPIC = KAFKA_ENV_VALUE + ".diku.NOTIFICATION_CALENDAR_TRIGGER";
  private static final long MATCH_TIMEOUT_MS = 60_000;
  private static final String DUE_IN_A_WEEK = "2099-03-08";
  private static final String DUE_TODAY = "2099-03-01";
  private static final String NOT_A_TARGET_DATE = "2099-03-02";

  private final List<String> createdInvoiceIds = new ArrayList<>();

  @AfterEach
  void deleteCreatedInvoices() throws MalformedURLException {
    for (var id : createdInvoiceIds) {
      deleteData(TestEntities.INVOICE.getEndpointWithId(), id).then().statusCode(204);
    }
    createdInvoiceIds.clear();
  }

  @Test
  void publishesUnpaidInvoicesDueOnTargetDates_andSkipsEventsForOtherModules() throws MalformedURLException {
    var dueToday = createInvoice("Open", DUE_TODAY);
    var dueInAWeek = createInvoice("Approved", DUE_IN_A_WEEK);
    createInvoice("Paid", DUE_TODAY);
    createInvoice("Cancelled", DUE_IN_A_WEEK);
    createInvoice("Open", NOT_A_TARGET_DATE);

    var ordersTrigger = triggerEvent("mod-orders-storage");
    var invoicesTrigger = triggerEvent("mod-invoice-storage");
    sendTrigger(ordersTrigger);
    sendTrigger(invoicesTrigger);

    var matchEvents = awaitMatchEvents(invoicesTrigger.getId());
    matchEvents.forEach(event -> {
      assertEquals("diku", event.getTenant());
      assertEquals("mod-invoice-storage", event.getSourceModule());
      assertEquals(DUE_TODAY, event.getBusinessDate());
    });
    var matches = matchEvents.stream()
      .flatMap(event -> event.getMatches().stream())
      .collect(Collectors.toMap(CalendarMatch::getRecordId, CalendarMatch::getTargetDate));
    assertEquals(Map.of(dueToday, DUE_TODAY, dueInAWeek, DUE_IN_A_WEEK), matches);
    assertTrue(matchEvents(ordersTrigger.getId()).isEmpty(), "a trigger for another module publishes nothing");
  }

  private String createInvoice(String status, String paymentDue) throws MalformedURLException {
    var invoice = new JsonObject(getFile(TestData.Invoice.DEFAULT))
      .put(ID, UUID.randomUUID().toString())
      .put("status", status)
      .put("paymentDue", paymentDue + "T00:00:00.000+00:00");
    postData(TestEntities.INVOICE.getEndpoint(), invoice.encode()).then().statusCode(201);
    createdInvoiceIds.add(invoice.getString(ID));
    return invoice.getString(ID);
  }

  private static CalendarTriggerEvent triggerEvent(String targetModule) {
    return new CalendarTriggerEvent()
      .withId(UUID.randomUUID().toString())
      .withTenant("diku")
      .withTargetModule(targetModule)
      .withBusinessDate(DUE_TODAY)
      .withRules(List.of(new CalendarTriggerRule()
        .withIntent("InvoicePaymentDue")
        .withTargetDates(List.of(DUE_TODAY, DUE_IN_A_WEEK))));
  }

  private static void sendTrigger(CalendarTriggerEvent event) {
    sendKafkaRecord(TRIGGER_TOPIC, event.getTargetModule(), Json.encode(event),
      Map.of("x-okapi-tenant", "diku", "x-folio-target-module", event.getTargetModule()));
  }

  private static List<CalendarMatchEvent> awaitMatchEvents(String triggerEventId) {
    var deadline = System.currentTimeMillis() + MATCH_TIMEOUT_MS;
    var events = matchEvents(triggerEventId);
    while (events.isEmpty() && System.currentTimeMillis() < deadline) {
      events = matchEvents(triggerEventId);
    }
    assertFalse(events.isEmpty(), "no match event for trigger event " + triggerEventId);
    return events;
  }

  private static List<CalendarMatchEvent> matchEvents(String triggerEventId) {
    return checkKafkaEventSent("diku", CALENDAR_MATCH_TOPIC).stream()
      .map(value -> Json.decodeValue(value, CalendarMatchEvent.class))
      .filter(event -> triggerEventId.equals(event.getTriggerEventId()))
      .toList();
  }

}
