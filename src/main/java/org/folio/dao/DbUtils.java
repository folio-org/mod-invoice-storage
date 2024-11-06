package org.folio.dao;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

import io.vertx.core.Future;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class DbUtils {

  private static final String TABLE_NAME_TEMPLATE = "%s.%s";

  public static String getTenantTableName(String tenantId, String tableName) {
    return TABLE_NAME_TEMPLATE.formatted(convertToPsqlStandard(tenantId), tableName);
  }

  public static Future<Void> verifyEntityUpdate(RowSet<Row> updated) {
    return updated.rowCount() == 1
      ? Future.succeededFuture()
      : Future.failedFuture(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
  }

}
