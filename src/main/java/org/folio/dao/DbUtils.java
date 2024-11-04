package org.folio.dao;

import static org.folio.rest.persist.PostgresClient.convertToPsqlStandard;

public class DbUtils {

  private static final String TABLE_NAME_TEMPLATE = "%s.%s";

  public static String getTenantTableName(String tenantId, String tableName) {
    return TABLE_NAME_TEMPLATE.formatted(convertToPsqlStandard(tenantId), tableName);
  }

}
