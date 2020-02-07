package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.BatchVoucher;
import org.folio.rest.jaxrs.resource.BatchVoucherStorage;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.BatchVoucherStorage.GetBatchVoucherStorageBatchVouchersByIdResponse.respond200WithApplicationJson;
import static org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceNumber.GetInvoiceStorageInvoiceNumberResponse.respond500WithTextPlain;


public class BatchVoucherAPI implements BatchVoucherStorage {
  private static final String BATCH_VOUCHER_TABLE = "vouchers";
  private static final Logger LOG = LoggerFactory.getLogger(BatchVoucherAPI.class);

  @Validate
  @Override
  public void postBatchVoucherStorageBatchVouchers(String lang, BatchVoucher entity, Map<String, String> okapiHeaders
            , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .save(BATCH_VOUCHER_TABLE, entity, reply -> {
          if (reply.succeeded()) {
            LOG.debug("Batch voucher was created: {}", entity.getId());
            asyncResultHandler.handle(
              succeededFuture(PostBatchVoucherStorageBatchVouchersResponse.respond201WithApplicationJson(entity)));
          } else {
            LOG.error(reply.cause().getMessage(), reply.cause());
            String msg = reply.cause().getMessage();
            asyncResultHandler.handle(
              succeededFuture(PostBatchVoucherStorageBatchVouchersResponse.respond500WithApplicationJson(msg)));
          }
        });
    });
  }

  @Validate
  @Override
  public void getBatchVoucherStorageBatchVouchersById(String id, String lang, Map<String, String> okapiHeaders
            , Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId).getById(BATCH_VOUCHER_TABLE, id, BatchVoucher.class, reply -> {
        if (reply.succeeded()) {
          BatchVoucher batchVoucher = reply.result();
          LOG.debug("Retrieved batch voucher: {}", batchVoucher);
          asyncResultHandler.handle(succeededFuture(respond200WithApplicationJson(batchVoucher)));
        } else {
          asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(reply.cause())));
        }
      });
    });
  }
}
