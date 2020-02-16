package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.ExportConfig;
import org.folio.rest.jaxrs.model.ExportConfigCollection;
import org.folio.rest.jaxrs.resource.BatchVoucherStorageExportConfigurations;
import org.folio.rest.persist.PgUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class BatchVoucherExportConfig implements BatchVoucherStorageExportConfigurations {

  public static final String BATCH_VOUCHER_EXPORT_CONFIGS_TABLE = "batch_voucher_export_configs";

  @Validate
  @Override
  public void getBatchVoucherStorageExportConfigurations(int offset, int limit, String query, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, ExportConfig.class, ExportConfigCollection.class, query, offset, limit,
        okapiHeaders, vertxContext,
        BatchVoucherStorageExportConfigurations.GetBatchVoucherStorageExportConfigurationsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postBatchVoucherStorageExportConfigurations(String lang, ExportConfig entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, entity, okapiHeaders, vertxContext,
        BatchVoucherStorageExportConfigurations.PostBatchVoucherStorageExportConfigurationsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getBatchVoucherStorageExportConfigurationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, ExportConfig.class, id, okapiHeaders, vertxContext,
        BatchVoucherStorageExportConfigurations.GetBatchVoucherStorageExportConfigurationsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteBatchVoucherStorageExportConfigurationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, id, okapiHeaders, vertxContext,
        BatchVoucherStorageExportConfigurations.DeleteBatchVoucherStorageExportConfigurationsByIdResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void putBatchVoucherStorageExportConfigurationsById(String id, String lang, ExportConfig entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, entity, id, okapiHeaders, vertxContext,
        BatchVoucherStorageExportConfigurations.PutBatchVoucherStorageExportConfigurationsByIdResponse.class, asyncResultHandler);
  }
}
