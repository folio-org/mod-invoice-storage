package org.folio.service.voucher;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.impl.BatchVoucherExportsImpl.BATCH_VOUCHER_EXPORTS_TABLE;
import static org.folio.rest.utils.ResponseUtils.handleNoContentResponse;

import javax.ws.rs.core.Response;
import java.util.Optional;

import org.folio.rest.jaxrs.model.BatchVoucherExport;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.CriterionBuilder;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.HttpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class BatchVoucherExportsService {

  private static final String BATCH_VOUCHER_ID = "batchVoucherId";
  private final PostgresClient pgClient;

  public void deleteBatchVoucherExportsById(String id, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {
    vertxContext.runOnContext(v -> pgClient.withTrans(conn -> getAssociatedBatchVoucherId(new BatchVoucherDeleteHolder().setBatchVoucherExportId(id), conn)
        .compose(holder -> deleteBatchVoucherExportById(holder, conn))
        .compose(holder -> new BatchVoucherService(pgClient).deleteBatchVoucherById(holder, conn)))
      .onComplete(handleNoContentResponse(asyncResultHandler,"Batch voucher exports {} {} deleted")));
  }

  private Future<BatchVoucherDeleteHolder> getAssociatedBatchVoucherId(BatchVoucherDeleteHolder holder, Conn conn) {
    var batchVoucherExportId = holder.getBatchVoucherExportId();
    log.debug("getAssociatedBatchVoucherId:: Trying to get batch voucher id for batch voucher export with id: {}", batchVoucherExportId);
    return conn.getById(BATCH_VOUCHER_EXPORTS_TABLE, batchVoucherExportId, BatchVoucherExport.class)
      .compose(batchVoucherExport -> Optional.ofNullable(batchVoucherExport)
        .map(Future::succeededFuture)
        .orElseGet(() -> Future.failedFuture(new IllegalStateException("Batch voucher export not found"))))
      .map(batchVoucherExport -> holder.setBatchVoucherId(batchVoucherExport.getBatchVoucherId()))
      .recover(t -> Future.failedFuture(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase())))
      .onSuccess(v -> log.info("getAssociatedBatchVoucherIdBatch:: Voucher id '{}' retrieved for batch voucher export with id", holder.getBatchVoucherId()))
      .onFailure(t -> log.error("Failed to get batch voucher id for batch voucher export id: {}", batchVoucherExportId, t));
  }

  private Future<BatchVoucherDeleteHolder> deleteBatchVoucherExportById(BatchVoucherDeleteHolder holder, Conn conn) {
    var batchVoucherExportId = holder.getBatchVoucherExportId();
    log.debug("deleteBatchVoucherExportById:: Trying to delete batch voucher export with id: {}", batchVoucherExportId);
    return conn.delete(BATCH_VOUCHER_EXPORTS_TABLE, batchVoucherExportId)
      .compose(rowSet -> {
        if (rowSet.rowCount() == 0) {
          log.warn("deleteBatchVoucherExportById:: No rows deleted for batch voucher export with id {}", batchVoucherExportId);
          return Future.failedFuture(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()));
        }
        return Future.succeededFuture(holder);
      })
      .onSuccess(v -> log.info("deleteBatchVoucherExportById:: Batch voucher export with id {} deleted", batchVoucherExportId))
      .onFailure(t -> log.error("Failed to delete batch voucher export with id: {}", batchVoucherExportId, t));
  }

  public Future<BatchVoucherDeleteHolder> deleteBatchVoucherExportsByBatchVoucherId(BatchVoucherDeleteHolder holder, Conn conn) {
    var batchVoucherId = holder.getBatchVoucherId();
    log.debug("deleteBatchVoucherExportsByBatchVoucherId:: Trying to delete batch voucher exports with batch voucher id: {}", batchVoucherId);
    return conn.delete(BATCH_VOUCHER_EXPORTS_TABLE, new CriterionBuilder().with(BATCH_VOUCHER_ID, batchVoucherId).build())
      .recover(throwable -> Future.failedFuture(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase())))
      .map(holder)
      .onSuccess(v -> log.info("deleteBatchVoucherExportsByBatchVoucherId:: Batch voucher exports with batch voucher id {} deleted", batchVoucherId))
      .onFailure(t -> log.warn("deleteBatchVoucherExportsByBatchVoucherId:: Failed to delete batch voucher exports with batch voucher id {}", batchVoucherId, t));
  }

}
