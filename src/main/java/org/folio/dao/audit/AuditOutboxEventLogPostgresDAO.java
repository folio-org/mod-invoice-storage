package org.folio.dao.audit;

import static org.folio.dao.DbUtils.getTenantTableName;

import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.OutboxEventLog;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.interfaces.Results;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AuditOutboxEventLogPostgresDAO implements AuditOutboxEventLogDAO {

  public static final String OUTBOX_TABLE_NAME = "outbox_event_log";
  private static final String BATCH_DELETE = "DELETE from %s where event_id = ANY ($1)";

  /**
   * Get all event logs from outbox table.
   *
   * @param conn     the sql connection from transaction
   * @param tenantId the tenant id
   * @return future of a list of fetched event logs
   */
  public Future<List<OutboxEventLog>> getEventLogs(Conn conn, String tenantId) {
    log.trace("getEventLogs:: Fetching event logs from outbox table for tenantId: '{}'", tenantId);
    var tableName = getTenantTableName(tenantId, OUTBOX_TABLE_NAME);
    return conn.get(tableName, OutboxEventLog.class, new Criterion().setLimit(new Limit(1000)))
      .map(Results::getResults)
      .onFailure(t -> log.warn("getEventLogs:: Failed to fetch event logs for tenantId: '{}'", tenantId, t));
  }

  /**
   * Saves event log to outbox table.
   *
   * @param conn     the sql connection from transaction
   * @param eventLog the event log to save
   * @param tenantId the tenant id
   * @return future of id of the inserted entity
   */
  public Future<String> saveEventLog(Conn conn, OutboxEventLog eventLog, String tenantId) {
    log.debug("saveEventLog:: Saving event log to outbox table with eventId: '{}'", eventLog.getEventId());
    var tableName = getTenantTableName(tenantId, OUTBOX_TABLE_NAME);
    return conn.save(tableName, eventLog)
      .onFailure(t -> log.warn("saveEventLog:: Failed to save event log with id: '{}'", eventLog.getEventId(), t));
  }

  /**
   * Deletes outbox logs by event ids in batch.
   *
   * @param conn     the sql connection from transaction
   * @param eventIds the event ids to delete
   * @param tenantId the tenant id
   * @return future of row count for deleted records
   */
  public Future<Integer> deleteEventLogs(Conn conn, List<String> eventIds, String tenantId) {
    log.debug("deleteEventLogs:: Deleting outbox logs by event ids in batch: '{}'", eventIds);
    var tableName = getTenantTableName(tenantId, OUTBOX_TABLE_NAME);
    var param = eventIds.stream().map(UUID::fromString).toArray(UUID[]::new);
    return conn.execute(BATCH_DELETE.formatted(tableName), Tuple.of(param))
      .map(SqlResult::rowCount)
      .onFailure(t -> log.warn("deleteEventLogs: Failed to delete event logs by ids: '{}'", eventIds, t));
  }

}
