package org.folio.dao.lock;

import static org.folio.dao.DbUtils.getTenantTableName;

import org.folio.rest.persist.Conn;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.Tuple;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class InternalLockPostgresDAO implements InternalLockDAO {

  private static final String LOCK_TABLE_NAME = "internal_lock";
  private static final String SELECT_WITH_LOCKING = "SELECT * FROM %s WHERE lock_name = $1 FOR UPDATE";

  /**
   * Performs SELECT FOR UPDATE statement in order to implement locking.
   * Lock released after the transaction is committed.
   *
   * @param conn connection with active transaction
   * @param lockName the lock name
   * @param tenantId the tenant id
   * @return future with 1 row if lock was acquired
   */
  public Future<Integer> selectWithLocking(Conn conn, String lockName, String tenantId) {
    log.debug("selectWithLocking:: Locking row with lockName: '{}'", lockName);
    var tableName = getTenantTableName(tenantId, LOCK_TABLE_NAME);
    return conn.execute(SELECT_WITH_LOCKING.formatted(tableName), Tuple.of(lockName))
      .map(SqlResult::rowCount)
      .onFailure(t -> log.warn("selectWithLocking:: Unable to select row with lockName: '{}'", lockName, t));
  }
}
