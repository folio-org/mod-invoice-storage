package org.folio.service.audit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.dao.audit.AuditOutboxEventLogDAO;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.OutboxEventLog;
import org.folio.rest.jaxrs.model.OutboxEventLog.EntityType;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.DBClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class AuditOutboxService {

  private final AuditOutboxEventLogDAO outboxEventLogDAO;
  private final AuditEventProducer producer;

  /**
   * Reads outbox event logs from DB and send them to Kafka and delete from outbox table in a single transaction.
   *
   * @param okapiHeaders the okapi headers
   * @param vertxContext the vertx context
   * @return future with integer how many records have been processed
   */
  public Future<Integer> processOutboxEventLogs(Map<String, String> okapiHeaders, Context vertxContext) {
    var tenantId = TenantTool.tenantId(okapiHeaders);
    return new DBClient(vertxContext, okapiHeaders).getPgClient()
      .withTrans(conn -> outboxEventLogDAO.getEventLogs(conn, tenantId)
        .compose(logs -> {
          if (CollectionUtils.isEmpty(logs)) {
            log.info("processOutboxEventLogs: No logs found in outbox table");
            return Future.succeededFuture(0);
          }
          log.info("processOutboxEventLogs: {} logs found in outbox table, sending to kafka", logs.size());
          return Future.join(sendEventLogsToKafka(logs, okapiHeaders))
            .map(logs.stream().map(OutboxEventLog::getEventId).toList())
            .compose(eventIds -> outboxEventLogDAO.deleteEventLogs(conn, eventIds, tenantId))
            .onSuccess(count -> log.info("processOutboxEventLogs:: {} logs have been deleted from outbox table", count))
            .onFailure(ex -> log.error("Logs deletion failed", ex));
        })
        .onSuccess(count -> log.info("processOutboxEventLogs:: Successfully processed outbox event logs: {}", count))
        .onFailure(ex -> log.error("Failed to process outbox event logs", ex)));
  }

  private List<Future<Void>> sendEventLogsToKafka(List<OutboxEventLog> eventLogs, Map<String, String> okapiHeaders) {
    return eventLogs.stream().map(eventLog ->
      switch (eventLog.getEntityType()) {
        case INVOICE -> {
          var wrapper = decodePayload(eventLog.getPayload(), Invoice.class);
          var action = InvoiceAuditEvent.Action.fromValue(eventLog.getAction());
          yield producer.sendInvoiceEvent(wrapper.getEntity(), wrapper.getOriginalEntity(), action, okapiHeaders);
        }
        case INVOICE_LINE -> {
          var wrapper = decodePayload(eventLog.getPayload(), InvoiceLine.class);
          var action = InvoiceLineAuditEvent.Action.fromValue(eventLog.getAction());
          yield producer.sendInvoiceLineEvent(wrapper.getEntity(), wrapper.getOriginalEntity(), action, okapiHeaders);
        }
      }).toList();
  }

  /**
   * Decode an outbox payload into a wrapper. Falls back to decoding the payload as a bare entity
   * for backwards compatibility with rows written before the wrapper format was introduced.
   */
  <T> AuditEntityWrapper<T> decodePayload(String payload, Class<T> entityClass) {
    var mapper = DatabindCodec.mapper();
    try {
      var wrapperType = mapper.getTypeFactory().constructParametricType(AuditEntityWrapper.class, entityClass);
      AuditEntityWrapper<T> wrapper = mapper.readValue(payload, wrapperType);
      if (wrapper.getEntity() != null) {
        return wrapper;
      }
    } catch (Exception ignored) {
      // fall through to legacy decoding
    }
    log.warn("decodePayload:: Falling back to legacy (bare-entity) outbox payload decoding");
    T entity = Json.decodeValue(payload, entityClass);
    return AuditEntityWrapper.of(entity, null);
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
    return saveInvoiceOutboxLog(conn, entity, null, action, okapiHeaders);
  }

  /**
   * Saves invoice outbox log capturing the pre-edit state.
   *
   * @param conn         connection in transaction
   * @param entity       the invoice (post-edit state)
   * @param original     the invoice before the edit; null for Create
   * @param action       the event action
   * @param okapiHeaders okapi headers
   */
  public Future<Void> saveInvoiceOutboxLog(Conn conn, Invoice entity, Invoice original, InvoiceAuditEvent.Action action, Map<String, String> okapiHeaders) {
    return saveOutboxLog(conn, okapiHeaders, action.value(), EntityType.INVOICE, entity.getId(), AuditEntityWrapper.of(entity, original));
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
    return saveInvoiceLineOutboxLog(conn, entity, null, action, okapiHeaders);
  }

  /**
   * Saves invoice line outbox log capturing the pre-edit state.
   *
   * @param conn         connection in transaction
   * @param entity       the invoice line (post-edit state)
   * @param original     the invoice line before the edit; null for Create
   * @param action       the event action
   * @param okapiHeaders okapi headers
   */
  public Future<Void> saveInvoiceLineOutboxLog(Conn conn, InvoiceLine entity, InvoiceLine original, InvoiceLineAuditEvent.Action action, Map<String, String> okapiHeaders) {
    return saveOutboxLog(conn, okapiHeaders, action.value(), EntityType.INVOICE_LINE, entity.getId(), AuditEntityWrapper.of(entity, original));
  }

  private Future<Void> saveOutboxLog(Conn conn, Map<String, String> okapiHeaders, String action, EntityType entityType, String entityId, AuditEntityWrapper<?> wrapper) {
    log.debug("saveOutboxLog:: Saving outbox log for {} with id: {}", entityType, entityId);
    var eventLog = new OutboxEventLog()
      .withEventId(UUID.randomUUID().toString())
      .withAction(action)
      .withEntityType(entityType)
      .withPayload(Json.encode(wrapper));
    return outboxEventLogDAO.saveEventLog(conn, eventLog, TenantTool.tenantId(okapiHeaders))
      .onSuccess(reply -> log.info("saveOutboxLog:: Outbox log has been saved for {} with id: {}", entityType, entityId))
      .onFailure(e -> log.warn("saveOutboxLog:: Could not save outbox audit log for {} with id: {}", entityType, entityId, e));
  }

}
