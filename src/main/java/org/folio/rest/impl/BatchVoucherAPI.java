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
  private static final String BATCH_VOUCHER_TABLE = "vouchers";

  @Override
  @Validate
  public void postBatchVoucherStorageBatchVouchers(String lang, BatchVoucher entity, Map<String, String> okapiHeaders
            , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
      PgUtil.post(BATCH_VOUCHER_TABLE, entity, okapiHeaders, vertxContext
        , PostBatchVoucherStorageBatchVouchersResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void getBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders
            , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHER_TABLE, BatchVoucher.class, id, okapiHeaders, vertxContext
        , GetBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders
        , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_VOUCHER_TABLE, id, okapiHeaders, vertxContext
        , DeleteBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }
}
