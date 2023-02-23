package org.folio.service;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.impl.BatchVoucherExportsImpl.BATCH_VOUCHER_EXPORTS_TABLE;
import static org.folio.rest.utils.ResponseUtils.handleNoContentResponse;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.BatchVoucherExport;
import org.folio.rest.persist.CriterionBuilder;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Tx;
import org.folio.rest.persist.Criteria.Criterion;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.HttpException;

public class BatchVoucherExportsService {

  private static final String BATCH_VOUCHER_EXPORT_ID = "batchVoucherExportId";
  private static final String BATCH_VOUCHER_ID = "batchVoucherId";
  private static final Logger log = LogManager.getLogger(BatchVoucherExportsService.class);
  private final PostgresClient pgClient;

  public BatchVoucherExportsService(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public void deleteBatchVoucherExportsById(String id, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {

    BatchVoucherService batchVoucherService = new BatchVoucherService(pgClient);

    vertxContext.runOnContext(v -> {
      Tx<Map<String, String>> tx = new Tx<>(new HashMap<>(Map.of(BATCH_VOUCHER_EXPORT_ID, id)), pgClient);

      tx.startTx()
        .compose(this::getAssociatedBatchVoucherId)
        .compose(this::deleteBatchVoucherExportById)
        .compose(batchVoucherService::deleteBatchVoucherById)
        .compose(Tx::endTx)
        .onComplete(handleNoContentResponse(asyncResultHandler, tx, "Batch voucher exports {} {} deleted"));
    });
  }

  public Future<Tx<Map<String, String>>> deleteBatchVoucherExportById(Tx<Map<String, String>> tx) {
    Promise<Tx<Map<String, String>>> promise = Promise.promise();
    String batchVoucherExportId = tx.getEntity().get(BATCH_VOUCHER_EXPORT_ID);
    log.debug("deleteBatchVoucherExportById:: Trying to delete batch voucher export with id: {}", batchVoucherExportId);
    pgClient.delete(tx.getConnection(), BATCH_VOUCHER_EXPORTS_TABLE, batchVoucherExportId, rs -> {
      log.info("deleteBatchVoucherExportById:: Deletion of batch voucher exports completed");
      if (rs.result().rowCount() == 0) {
        log.warn("deleteBatchVoucherExportById:: No rows deleted for batch voucher export");
        promise.fail(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
      } else {
        log.error("Failed to delete batch voucher export", rs.cause());
        promise.complete(tx);
      }
    });
    return promise.future();
  }

  public Future<Tx<Map<String, String>>> deleteBatchVoucherExportsByBatchVoucherId(Tx<Map<String, String>> tx) {
    Promise<Tx<Map<String, String>>> promise = Promise.promise();
    String batchVoucherId = tx.getEntity().get(BATCH_VOUCHER_ID);
    log.debug("deleteBatchVoucherExportsByBatchVoucherId:: Trying to delete batch voucher exports with batch voucher id: {}", batchVoucherId);
    Criterion criterion = new CriterionBuilder().with(BATCH_VOUCHER_ID, batchVoucherId).build();

    pgClient.delete(tx.getConnection(), BATCH_VOUCHER_EXPORTS_TABLE, criterion, rs -> {
      log.info("deleteBatchVoucherExportsByBatchVoucherId:: Deletion of batch voucher exports completed");
      if (rs.failed()) {
        log.warn("deleteBatchVoucherExportsByBatchVoucherId:: Failed to delete batch voucher exports with batch voucher id {}: {}", batchVoucherId, rs.cause());
        promise.fail(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
      } else {
        log.info("deleteBatchVoucherExportsByBatchVoucherId:: Batch voucher exports with batch voucher id {} deleted", batchVoucherId);
        promise.complete(tx);
      }
    });
    return promise.future();
  }

  public Future<Tx<Map<String, String>>> getAssociatedBatchVoucherId(Tx<Map<String, String>> tx) {
    Promise<Tx<Map<String, String>>> promise = Promise.promise();
    String batchVoucherExportId = tx.getEntity().get(BATCH_VOUCHER_EXPORT_ID);
    log.debug("getAssociatedBatchVoucherId:: Trying to get batch voucher id for batch voucher export with id: {}", batchVoucherExportId);
    pgClient.getById(BATCH_VOUCHER_EXPORTS_TABLE, batchVoucherExportId, BatchVoucherExport.class, rs -> {
      if (rs.failed() || rs.result() == null) {
        log.error("Failed to get batch voucher id for batch voucher export id: {}", batchVoucherExportId, rs.cause());
        promise.fail(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
      } else {
        log.info("getAssociatedBatchVoucherIdBatch:: voucher id '{}' retrieved for batch voucher export with id", rs.result().getBatchVoucherId());
        tx.getEntity().put(BATCH_VOUCHER_ID, rs.result().getBatchVoucherId());
        promise.complete(tx);
      }
    });
    return promise.future();
  }
}
