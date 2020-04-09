package org.folio.service;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.util.ResponseUtils.handleNoContentResponse;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Tx;

public class BatchVoucherService {

  private static final String BATCH_VOUCHER_ID = "batchVoucherId";
  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private PostgresClient pgClient;

  public BatchVoucherService(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public void deleteBatchVoucherById(String id, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {

    BatchVoucherExportsService batchVoucherExportsService = new BatchVoucherExportsService(pgClient);

    vertxContext.runOnContext((v) -> {
      Tx<Map<String, String>> tx = new Tx<>(ImmutableMap.of(BATCH_VOUCHER_ID, id), pgClient);

      tx.startTx()
        .compose(batchVoucherExportsService::deleteBatchVoucherExportsByBatchVoucherId)
        .compose(this::deleteBatchVoucherById)
        .compose(Tx::endTx)
        .setHandler(handleNoContentResponse(asyncResultHandler, tx, "Batch vouchers {} {} deleted"));
    });
  }

  public Future<Tx<Map<String, String>>> deleteBatchVoucherById(Tx<Map<String, String>> tx) {
    Promise<Tx<Map<String, String>>> promise = Promise.promise();

    pgClient.delete(tx.getConnection(), BATCH_VOUCHERS_TABLE, tx.getEntity().get(BATCH_VOUCHER_ID), (rs) -> {
      logger.info("deletion of batch voucher completed");
      if (rs.result().getUpdated() == 0) {
        promise.fail(new HttpStatusException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
      } else {
        promise.complete(tx);
      }
    });
    return promise.future();
  }
}