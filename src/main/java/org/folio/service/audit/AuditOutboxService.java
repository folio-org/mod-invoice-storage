package org.folio.service.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dao.PostgresClientFactory;
import org.folio.dao.audit.AuditOutboxEventLogDAO;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.OutboxEventLog;
import org.folio.rest.jaxrs.model.OutboxEventLog.EntityType;
import org.folio.rest.persist.Conn;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class AuditOutboxService {

  private final AuditOutboxEventLogDAO outboxEventLogDAO;
  private final AuditEventProducer producer;
  private final PostgresClientFactory pgClientFactory;

  /**
   * Reads outbox event logs from DB and send them to Kafka and delete from outbox table in a single transaction.
   *
   * @param okapiHeaders the okapi headers
   * @return future with integer how many records have been processed
   */
  public Future<Integer> processOutboxEventLogs(Map<String, String> okapiHeaders) {
    var tenantId = TenantTool.tenantId(okapiHeaders);
    log.trace("processOutboxEventLogs:: Preparing to process outbox event logs, tenantId={}", tenantId);
    var pgClient = pgClientFactory.createInstance(tenantId);
    return pgClient.withTrans(conn -> outboxEventLogDAO.getEventLogs(conn, tenantId)
        .compose(logs -> {
          if (CollectionUtils.isEmpty(logs)) {
            log.info("processOutboxEventLogs:: No logs found in outbox table");
            return Future.succeededFuture(0);
          }
          log.info("processOutboxEventLogs:: {} logs found in outbox table, sending to Kafka", logs.size());
          var futures = sendEventLogsToKafka(logs, okapiHeaders);

          // Wait for all futures to complete (succeed or fail)
          return Future.all(futures)
            .recover(t -> Future.succeededFuture()) // Recover so we can process partial successes
            .compose(v -> {
              // Only collect event IDs for successfully sent events
              var successfulEventIds = new ArrayList<String>();
              for (int i = 0; i < futures.size(); i++) {
                if (futures.get(i).succeeded()) {
                  successfulEventIds.add(logs.get(i).getEventId());
                }
              }

              if (CollectionUtils.isEmpty(successfulEventIds)) {
                log.debug("processOutboxEventLogs:: No events were successfully sent to Kafka");
                return Future.succeededFuture(0);
              }

              log.info("processOutboxEventLogs:: Successfully sent {} out of {} events to Kafka", successfulEventIds.size(), logs.size());
              return outboxEventLogDAO.deleteEventLogs(conn, successfulEventIds, tenantId)
                .onSuccess(count -> log.info("processOutboxEventLogs:: Logs deleted from outbox table: {}", count))
                .onFailure(ex -> log.error("Logs deletion failed", ex));
            });
        })
        .onSuccess(count -> log.info("processOutboxEventLogs:: Successfully processed outbox event logs: {}", count))
        .onFailure(ex -> log.error("Failed to process outbox event logs", ex)));
  }

  private List<Future<Void>> sendEventLogsToKafka(List<OutboxEventLog> eventLogs, Map<String, String> okapiHeaders) {
    return eventLogs.stream().<Future<Void>>map(eventLog -> {
      try {
        return switch (eventLog.getEntityType()) {
          case INVOICE -> {
            var invoice = Json.decodeValue(eventLog.getPayload(), Invoice.class);
            var action = InvoiceAuditEvent.Action.fromValue(eventLog.getAction());
            yield producer.sendInvoiceEvent(invoice, action, okapiHeaders);
          }
          case INVOICE_LINE -> {
            var invoiceLine = Json.decodeValue(eventLog.getPayload(), InvoiceLine.class);
            var action = InvoiceLineAuditEvent.Action.fromValue(eventLog.getAction());
            yield producer.sendInvoiceLineEvent(invoiceLine, action, okapiHeaders);
          }
        };
      } catch (Exception e) {
        log.warn("sendEventLogsToKafka:: Failed to process event with id: {}", eventLog.getEventId(), e);
        return Future.failedFuture(e);
      }
    }).toList();
  }

  /**
   * Saves invoice outbox log.
   *
   * @param conn         connection in transaction
   * @param entity       the invoice
   * @param action       the event action
   * @param okapiHeaders okapi headers
   * @return future with saved outbox log id in the same transaction
   */
  public Future<Void> saveInvoiceOutboxLog(Conn conn, Invoice entity, InvoiceAuditEvent.Action action, Map<String, String> okapiHeaders) {
    return saveOutboxLog(conn, okapiHeaders, action.value(), EntityType.INVOICE, entity.getId(), entity);
  }

  /**
   * Saves invoice line outbox log.
   *
   * @param conn         connection in transaction
   * @param entity       the invoice line
   * @param action       the event action
   * @param okapiHeaders okapi headers
   * @return future with saved outbox log id in the same transaction
   */
  public Future<Void> saveInvoiceLineOutboxLog(Conn conn, InvoiceLine entity, InvoiceLineAuditEvent.Action action, Map<String, String> okapiHeaders) {
    return saveOutboxLog(conn, okapiHeaders, action.value(), EntityType.INVOICE_LINE, entity.getId(), entity);
  }

  private Future<Void> saveOutboxLog(Conn conn, Map<String, String> okapiHeaders, String action, EntityType entityType, String entityId, Object entity) {
    log.debug("saveOutboxLog:: Saving outbox log for {} with id: {}", entityType, entityId);
    var eventLog = new OutboxEventLog()
      .withEventId(UUID.randomUUID().toString())
      .withAction(action)
      .withEntityType(entityType)
      .withPayload(Json.encode(entity));
    return outboxEventLogDAO.saveEventLog(conn, eventLog, TenantTool.tenantId(okapiHeaders))
      .onSuccess(reply -> log.info("saveOutboxLog:: Outbox log has been saved for {} with id: {}", entityType, entityId))
      .onFailure(e -> log.warn("saveOutboxLog:: Could not save outbox audit log for {} with id: {}", entityType, entityId, e));
  }

}
