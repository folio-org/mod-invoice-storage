package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.BatchVoucherExport;
import org.folio.rest.jaxrs.model.BatchVoucherExportCollection;
import org.folio.rest.jaxrs.resource.BatchVoucherStorageBatchVoucherExports;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.BatchVoucherExportsService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;


public class BatchVoucherExportsImpl implements BatchVoucherStorageBatchVoucherExports {

  public static final String BATCH_VOUCHER_EXPORTS_TABLE = "batch_voucher_exports";

  private final BatchVoucherExportsService batchVoucherExportsService;

  public BatchVoucherExportsImpl(Vertx vertx, String tenantId) {
    this.batchVoucherExportsService = new BatchVoucherExportsService(PostgresClient.getInstance(vertx, tenantId));
  }

  @Validate
  @Override
  public void getBatchVoucherStorageBatchVoucherExports(int offset, int limit, String query, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(BATCH_VOUCHER_EXPORTS_TABLE, BatchVoucherExport.class, BatchVoucherExportCollection.class, query, offset, limit,
      okapiHeaders, vertxContext, BatchVoucherStorageBatchVoucherExports.GetBatchVoucherStorageBatchVoucherExportsResponse.class,
      asyncResultHandler);
  }

  @Validate
  @Override
  public void postBatchVoucherStorageBatchVoucherExports(String lang, BatchVoucherExport entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_VOUCHER_EXPORTS_TABLE, entity, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.PostBatchVoucherStorageBatchVoucherExportsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getBatchVoucherStorageBatchVoucherExportsById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHER_EXPORTS_TABLE, BatchVoucherExport.class, id, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.GetBatchVoucherStorageBatchVoucherExportsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteBatchVoucherStorageBatchVoucherExportsById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    batchVoucherExportsService.deleteBatchVoucherExportsById(id, vertxContext, asyncResultHandler);
  }

  @Validate
  @Override
  public void putBatchVoucherStorageBatchVoucherExportsById(String id, String lang, BatchVoucherExport entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(BATCH_VOUCHER_EXPORTS_TABLE, entity, id, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.PutBatchVoucherStorageBatchVoucherExportsByIdResponse.class, asyncResultHandler);
  }
}
