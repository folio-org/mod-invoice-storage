package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.BatchVoucher;

import org.folio.rest.jaxrs.resource.BatchVoucherStorageBatchVouchers;
import org.folio.rest.persist.PgUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class BatchVouchersImpl implements BatchVoucherStorageBatchVouchers {
  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";

  @Validate
  @Override
  public void postBatchVoucherStorageBatchVouchers(String lang, BatchVoucher entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_VOUCHERS_TABLE, entity, okapiHeaders, vertxContext,
        BatchVoucherStorageBatchVouchers.PostBatchVoucherStorageBatchVouchersResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHERS_TABLE, BatchVoucher.class, id, okapiHeaders, vertxContext,
        BatchVoucherStorageBatchVouchers.GetBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_VOUCHERS_TABLE, id, okapiHeaders, vertxContext,
        BatchVoucherStorageBatchVouchers.DeleteBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }
}
