package org.folio.service;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.util.ResponseUtils.handleNoContentResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Tx;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.HttpException;

public class BatchVoucherService {

  private static final String BATCH_VOUCHER_ID = "batchVoucherId";
  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";
  private static final Logger logger = LogManager.getLogger(BatchVoucherService.class);
  private final PostgresClient pgClient;

  public BatchVoucherService(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public void deleteBatchVoucherById(String id, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {

    BatchVoucherExportsService batchVoucherExportsService = new BatchVoucherExportsService(pgClient);

    vertxContext.runOnContext(v -> {
      Tx<Map<String, String>> tx = new Tx<>(ImmutableMap.of(BATCH_VOUCHER_ID, id), pgClient);

      tx.startTx()
        .compose(batchVoucherExportsService::deleteBatchVoucherExportsByBatchVoucherId)
        .compose(this::deleteBatchVoucherById)
        .compose(Tx::endTx)
        .onComplete(handleNoContentResponse(asyncResultHandler, tx, "Batch vouchers {} {} deleted"));
    });
  }

  public Future<Tx<Map<String, String>>> deleteBatchVoucherById(Tx<Map<String, String>> tx) {
    Promise<Tx<Map<String, String>>> promise = Promise.promise();

    pgClient.delete(tx.getConnection(), BATCH_VOUCHERS_TABLE, tx.getEntity().get(BATCH_VOUCHER_ID), rs -> {
      logger.info("deletion of batch voucher completed");
      if (rs.result().rowCount() == 0) {
        promise.fail(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
      } else {
        promise.complete(tx);
      }
    });
    return promise.future();
  }
}
