package org.folio.dao;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

import javax.ws.rs.core.Response;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.experimental.UtilityClass;

@UtilityClass
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

  public static <T> T convertResponseToEntity(Response response, Class<T> entityClass) {
    try {
      return JsonObject.mapFrom(response.getEntity()).mapTo(entityClass);
    } catch (RuntimeException e) {
      throw new IllegalStateException(String.format("Cannot convert response '%s' to entity '%s' - error message: %s",
        response.getEntity(), entityClass.getName(), e.getMessage()));
    }
  }

}
