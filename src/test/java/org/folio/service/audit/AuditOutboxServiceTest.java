package org.folio.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.folio.CopilotGenerated;
import org.folio.dao.PostgresClientFactory;
import org.folio.dao.audit.AuditOutboxEventLogDAO;
import org.folio.rest.jaxrs.model.OutboxEventLog;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Context;
import io.vertx.core.Future;

@CopilotGenerated(model = "Claude Sonnet 4.5")
@ExtendWith(MockitoExtension.class)
public class AuditOutboxServiceTest {

  @Mock
  private AuditOutboxEventLogDAO outboxEventLogDAO;
  @Mock
  private AuditEventProducer producer;
  @Mock
  private PostgresClientFactory pgClientFactory;
  @Mock
  private PostgresClient pgClient;
  @Mock
  private Conn conn;
  @Mock
  private Context vertxContext;

  @InjectMocks
  private AuditOutboxService auditOutboxService;

  private Map<String, String> okapiHeaders;

  @BeforeEach
  void setUp() {
    okapiHeaders = Map.of("x-okapi-tenant", "testTenant");
    when(pgClientFactory.createInstance(any())).thenReturn(pgClient);
    when(pgClient.withTrans(any())).thenAnswer(invocation -> invocation.<Function<Conn, Future<?>>>getArgument(0).apply(conn));
  }

  @Test
  void processOutboxEventLogs_handlesEmptyLogsGracefully() {
    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of()));

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    assertTrue(result.succeeded());
    assertEquals(0, result.result());
  }

  @Test
  void processOutboxEventLogs_sendsInvoiceEventsAndDeletesLogs() {
    OutboxEventLog eventLog = new OutboxEventLog()
      .withEventId("eventId")
      .withEntityType(OutboxEventLog.EntityType.INVOICE)
      .withAction("Create")
      .withPayload("{\"id\":\"inv-123\",\"folioInvoiceNo\":\"INV-001\"}");
    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(eventLog)));
    when(outboxEventLogDAO.deleteEventLogs(any(), any(), any())).thenReturn(Future.succeededFuture(1));
    when(producer.sendInvoiceEvent(any(), any(), any())).thenReturn(Future.succeededFuture());

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    assertTrue(result.succeeded());
    assertEquals(1, result.result());
  }

  @Test
  void processOutboxEventLogs_sendsInvoiceLineEventsAndDeletesLogs() {
    OutboxEventLog eventLog = new OutboxEventLog()
      .withEventId("eventId")
      .withEntityType(OutboxEventLog.EntityType.INVOICE_LINE)
      .withAction("Edit")
      .withPayload("{\"id\":\"line-123\",\"invoiceId\":\"inv-123\"}");
    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(eventLog)));
    when(outboxEventLogDAO.deleteEventLogs(any(), any(), any())).thenReturn(Future.succeededFuture(1));
    when(producer.sendInvoiceLineEvent(any(), any(), any())).thenReturn(Future.succeededFuture());

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    assertTrue(result.succeeded());
    assertEquals(1, result.result());
  }

  @Test
  void processOutboxEventLogs_handlesProducerFailure() {
    OutboxEventLog eventLog = new OutboxEventLog()
      .withEventId("eventId")
      .withEntityType(OutboxEventLog.EntityType.INVOICE)
      .withAction("Create")
      .withPayload("{\"id\":\"inv-123\",\"folioInvoiceNo\":\"INV-001\"}");
    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(eventLog)));
    when(producer.sendInvoiceEvent(any(), any(), any())).thenReturn(Future.failedFuture(new RuntimeException("Producer error")));

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    // Wait for future to complete
    while (!result.isComplete()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    // Producer exceptions are now caught and handled gracefully
    assertTrue(result.succeeded());
    assertEquals(0, result.result()); // No events successfully processed
  }

  @Test
  void processOutboxEventLogs_handlesInvalidEntityType() {
    OutboxEventLog eventLog = new OutboxEventLog()
      .withEventId("eventId")
      .withEntityType(null)
      .withAction("Create")
      .withPayload("{\"id\":\"inv-123\"}");
    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(eventLog)));

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    assertTrue(result.succeeded());
    assertEquals(0, result.result());
  }

  @Test
  void processOutboxEventLogs_handlesMissingMetadata() {
    OutboxEventLog eventLog = new OutboxEventLog()
      .withEventId("eventId")
      .withEntityType(OutboxEventLog.EntityType.INVOICE)
      .withAction("Edit")
      .withPayload("{}");
    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(eventLog)));
    when(producer.sendInvoiceEvent(any(), any(), any())).thenReturn(Future.failedFuture(new IllegalArgumentException("Metadata is null")));

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    // The event should be skipped gracefully, not throw an exception
    assertTrue(result.succeeded());
    assertEquals(0, result.result()); // No events successfully processed
  }

  @Test
  void processOutboxEventLogs_handlesPartialSuccess() {
    OutboxEventLog eventLog1 = new OutboxEventLog()
      .withEventId("eventId1")
      .withEntityType(OutboxEventLog.EntityType.INVOICE)
      .withAction("Create")
      .withPayload("{\"id\":\"inv-123\",\"folioInvoiceNo\":\"INV-001\"}");
    OutboxEventLog eventLog2 = new OutboxEventLog()
      .withEventId("eventId2")
      .withEntityType(OutboxEventLog.EntityType.INVOICE_LINE)
      .withAction("Create")
      .withPayload("{\"id\":\"line-123\",\"invoiceId\":\"inv-123\"}");

    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(eventLog1, eventLog2)));
    when(producer.sendInvoiceEvent(any(), any(), any())).thenReturn(Future.succeededFuture());
    when(producer.sendInvoiceLineEvent(any(), any(), any())).thenReturn(Future.failedFuture(new RuntimeException("Failed")));
    when(outboxEventLogDAO.deleteEventLogs(any(), any(), any())).thenReturn(Future.succeededFuture(1));

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    // Wait for future to complete
    while (!result.isComplete()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    assertTrue(result.succeeded());
    assertEquals(1, result.result()); // Only 1 event successfully processed
  }

  @Test
  void processOutboxEventLogs_handlesMixedEntityTypes() {
    OutboxEventLog invoiceLog = new OutboxEventLog()
      .withEventId("eventId1")
      .withEntityType(OutboxEventLog.EntityType.INVOICE)
      .withAction("Create")
      .withPayload("{\"id\":\"inv-123\",\"folioInvoiceNo\":\"INV-001\"}");
    OutboxEventLog lineLog = new OutboxEventLog()
      .withEventId("eventId2")
      .withEntityType(OutboxEventLog.EntityType.INVOICE_LINE)
      .withAction("Create")
      .withPayload("{\"id\":\"line-123\",\"invoiceId\":\"inv-123\"}");

    when(outboxEventLogDAO.getEventLogs(any(), any())).thenReturn(Future.succeededFuture(List.of(invoiceLog, lineLog)));
    when(producer.sendInvoiceEvent(any(), any(), any())).thenReturn(Future.succeededFuture());
    when(producer.sendInvoiceLineEvent(any(), any(), any())).thenReturn(Future.succeededFuture());
    when(outboxEventLogDAO.deleteEventLogs(any(), any(), any())).thenReturn(Future.succeededFuture(2));

    Future<Integer> result = auditOutboxService.processOutboxEventLogs(okapiHeaders);

    // Wait for future to complete
    while (!result.isComplete()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    assertTrue(result.succeeded());
    assertEquals(2, result.result()); // Both events successfully processed
  }
}
