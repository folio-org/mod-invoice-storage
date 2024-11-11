package org.folio.dao.lock;

import org.folio.rest.persist.Conn;

import io.vertx.core.Future;

public interface InternalLockDAO {

  Future<Integer> selectWithLocking(Conn conn, String lockName, String tenantId);

}
