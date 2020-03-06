package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.model.BatchVoucherExport;
import org.folio.rest.jaxrs.model.BatchVoucherExportCollection;
import org.folio.rest.jaxrs.resource.BatchVoucherStorageBatchVoucherExports;
import org.folio.rest.persist.PgUtil;

import javax.ws.rs.core.Response;
import java.util.Map;


public class BatchVoucherExportsImpl implements BatchVoucherStorageBatchVoucherExports {

  public static final String BATCH_VOUCHER_EXPORTS_TABLE = "batch_voucher_exports";

  @Override
  public void getBatchVoucherStorageBatchVoucherExports(int offset, int limit, String query, String lang,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(BATCH_VOUCHER_EXPORTS_TABLE, BatchVoucherExport.class, BatchVoucherExportCollection.class, query, offset, limit,
      okapiHeaders, vertxContext, BatchVoucherStorageBatchVoucherExports.GetBatchVoucherStorageBatchVoucherExportsResponse.class,
      asyncResultHandler);
  }

  @Override
  public void postBatchVoucherStorageBatchVoucherExports(String lang, BatchVoucherExport entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_VOUCHER_EXPORTS_TABLE, entity, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.PostBatchVoucherStorageBatchVoucherExportsResponse.class, asyncResultHandler);
  }

  @Override
  public void getBatchVoucherStorageBatchVoucherExportsById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHER_EXPORTS_TABLE, BatchVoucherExport.class, id, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.GetBatchVoucherStorageBatchVoucherExportsByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteBatchVoucherStorageBatchVoucherExportsById(String id, String lang, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_VOUCHER_EXPORTS_TABLE, id, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.DeleteBatchVoucherStorageBatchVoucherExportsByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putBatchVoucherStorageBatchVoucherExportsById(String id, String lang, BatchVoucherExport entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(BATCH_VOUCHER_EXPORTS_TABLE, entity, id, okapiHeaders, vertxContext,
      BatchVoucherStorageBatchVoucherExports.PutBatchVoucherStorageBatchVoucherExportsByIdResponse.class, asyncResultHandler);
  }
}
