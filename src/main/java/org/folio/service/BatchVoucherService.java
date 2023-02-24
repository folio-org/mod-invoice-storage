package org.folio.service;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.utils.ResponseUtils.handleNoContentResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Tx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.HttpException;

public class BatchVoucherService {

  private static final String BATCH_VOUCHER_ID = "batchVoucherId";
  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";
  private static final Logger log = LogManager.getLogger(BatchVoucherService.class);
  private final PostgresClient pgClient;

  public BatchVoucherService(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public void deleteBatchVoucherById(String id, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {
    log.debug("deleteBatchVoucherById:: Trying to delete batch voucher with id: {}", id);
    BatchVoucherExportsService batchVoucherExportsService = new BatchVoucherExportsService(pgClient);

    vertxContext.runOnContext(v -> {
      Tx<Map<String, String>> tx = new Tx<>(Map.of(BATCH_VOUCHER_ID, id), pgClient);

      tx.startTx()
        .compose(batchVoucherExportsService::deleteBatchVoucherExportsByBatchVoucherId)
        .compose(this::deleteBatchVoucherById)
        .compose(Tx::endTx)
        .onComplete(handleNoContentResponse(asyncResultHandler, tx, "Batch vouchers {} {} deleted"));
    });
  }

  public Future<Tx<Map<String, String>>> deleteBatchVoucherById(Tx<Map<String, String>> tx) {
    Promise<Tx<Map<String, String>>> promise = Promise.promise();
    String batchVoucherId = tx.getEntity().get(BATCH_VOUCHER_ID);
    log.debug("deleteBatchVoucherById:: Trying to delete batch voucher with id: {}", batchVoucherId);
    pgClient.delete(tx.getConnection(), BATCH_VOUCHERS_TABLE, batchVoucherId, rs -> {
      log.info("deleteBatchVoucherById:: deletion of batch voucher completed");
      if (rs.result().rowCount() == 0) {
        log.warn("deleteBatchVoucherById:: Batch voucher with id '{}' not found", batchVoucherId);
        promise.fail(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
      } else {
        log.info("deleteBatchVoucherById:: Batch voucher with id '{}' deleted", tx.getEntity().get(BATCH_VOUCHER_ID));
        promise.complete(tx);
      }
    });
    return promise.future();
  }
}
