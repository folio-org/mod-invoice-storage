package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.BatchVoucher;
import org.folio.rest.jaxrs.resource.BatchVoucherStorage;
import org.folio.rest.persist.PgUtil;

import javax.ws.rs.core.Response;
import java.util.Map;

public class BatchVoucherAPI implements BatchVoucherStorage {
  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";

  @Override
  @Validate
  public void postBatchVoucherStorageBatchVouchers(String lang, BatchVoucher entity, Map<String, String> okapiHeaders
            , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
      PgUtil.post(BATCH_VOUCHERS_TABLE, entity, okapiHeaders, vertxContext
        , PostBatchVoucherStorageBatchVouchersResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void getBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders
            , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHERS_TABLE, BatchVoucher.class, id, okapiHeaders, vertxContext
        , GetBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders
        , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_VOUCHERS_TABLE, id, okapiHeaders, vertxContext
        , DeleteBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }
}
