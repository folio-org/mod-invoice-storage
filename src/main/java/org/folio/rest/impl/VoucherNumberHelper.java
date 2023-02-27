package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;

public class VoucherNumberHelper {

  private static final Logger log = LogManager.getLogger(VoucherNumberHelper.class);

  public void retrieveVoucherNumber(AsyncResult<Row> reply, Handler<AsyncResult<Response>> asyncResultHandler,
    Messages messages, String lang) {
    log.debug("retrieveVoucherNumber:: Trying to get voucher number");
    try {
      if (reply.succeeded()) {
        String voucherNumber = reply.result().getLong(0).toString();
        log.info("retrieveVoucherNumber:: Retrieved voucher number: {}", voucherNumber);
        SequenceNumber sequenceNumber = new SequenceNumber().withSequenceNumber(voucherNumber);
        asyncResultHandler.handle(succeededFuture(
            VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond200WithApplicationJson(sequenceNumber)));
      } else {
        log.error("retrieveVoucherNumber:: Failed to retrieve voucher number", reply.cause());
        String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
        asyncResultHandler
          .handle(succeededFuture(VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain(msg)));
      }
    } catch (Exception e) {
      log.error("Error while handling response for voucher number request", e);
      String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
      asyncResultHandler
        .handle(succeededFuture(VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain(msg)));
    }
  }
}
