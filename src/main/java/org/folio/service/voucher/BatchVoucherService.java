package org.folio.service.voucher;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.utils.ResponseUtils.handleNoContentResponse;

import javax.ws.rs.core.Response;

import org.folio.rest.persist.Conn;
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
public class BatchVoucherService {

  private static final String BATCH_VOUCHERS_TABLE = "batch_vouchers";
  private final PostgresClient pgClient;

  public void deleteBatchVoucherById(String id, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {
    log.debug("deleteBatchVoucherById:: Trying to delete batch voucher with id: {}", id);
    vertxContext.runOnContext(v -> pgClient.withTrans(conn -> new BatchVoucherExportsService(pgClient)
        .deleteBatchVoucherExportsByBatchVoucherId(new BatchVoucherDeleteHolder().setBatchVoucherId(id), conn)
        .compose(holder -> deleteBatchVoucherById(holder, conn)))
      .onComplete(handleNoContentResponse(asyncResultHandler, "Batch vouchers {} {} deleted")));
  }

  public Future<BatchVoucherDeleteHolder> deleteBatchVoucherById(BatchVoucherDeleteHolder holder, Conn conn) {
    var batchVoucherId = holder.getBatchVoucherId();
    log.debug("deleteBatchVoucherById:: Deleting batch voucher with id: {}", batchVoucherId);
    return conn.delete(BATCH_VOUCHERS_TABLE, batchVoucherId)
      .compose(rowSet -> rowSet.rowCount() == 0
        ? Future.failedFuture(new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase()))
        : Future.succeededFuture(holder))
      .onSuccess(v -> log.info("deleteBatchVoucherById:: Successfully deleted batch voucher with id: {}", batchVoucherId))
      .onFailure(e -> log.error("deleteBatchVoucherById:: Deleting batch voucher with id {} failed", batchVoucherId, e));
  }

}
