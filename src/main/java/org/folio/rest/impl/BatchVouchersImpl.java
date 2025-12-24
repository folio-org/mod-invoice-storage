package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.BatchVoucher;
import org.folio.rest.jaxrs.resource.BatchVoucherStorageBatchVouchers;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.voucher.BatchVoucherService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class BatchVouchersImpl implements BatchVoucherStorageBatchVouchers {
  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";

  private final BatchVoucherService batchVoucherService;

  public BatchVouchersImpl(Vertx vertx, String tenantId) {
    this.batchVoucherService = new BatchVoucherService(PostgresClient.getInstance(vertx, tenantId));
  }

  @Validate
  @Override
  public void postBatchVoucherStorageBatchVouchers(BatchVoucher entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_VOUCHERS_TABLE, entity, okapiHeaders, vertxContext,
        BatchVoucherStorageBatchVouchers.PostBatchVoucherStorageBatchVouchersResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getBatchVoucherStorageBatchVouchersById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHERS_TABLE, BatchVoucher.class, id, okapiHeaders, vertxContext,
        BatchVoucherStorageBatchVouchers.GetBatchVoucherStorageBatchVouchersByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteBatchVoucherStorageBatchVouchersById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    batchVoucherService.deleteBatchVoucherById(id, vertxContext, asyncResultHandler);
  }
}
