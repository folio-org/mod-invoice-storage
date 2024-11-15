package org.folio.dao.audit;

import java.util.List;

import org.folio.rest.jaxrs.model.OutboxEventLog;
import org.folio.rest.persist.Conn;

import io.vertx.core.Future;

public interface AuditOutboxEventLogDAO {

  Future<List<OutboxEventLog>> getEventLogs(Conn conn, String tenantId);

  Future<Void> saveEventLog(Conn conn, OutboxEventLog eventLog, String tenantId);

  Future<Integer> deleteEventLogs(Conn conn, List<String> eventIds, String tenantId);

}
