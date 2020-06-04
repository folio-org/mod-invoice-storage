package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.sqlclient.Row;
import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VoucherNumberHelper {

  private static final Logger log = LoggerFactory.getLogger(VoucherNumberHelper.class);

  public static final String OKAPI_URL = "X-Okapi-Url";

  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  VoucherNumberHelper(Map<String, String> okapiHeaders) {
    getHttpClient(okapiHeaders);
  }

  public void retrieveVoucherNumber(AsyncResult<Row> reply, Handler<AsyncResult<Response>> asyncResultHandler,
    Messages messages, String lang) {
    try {
      if (reply.succeeded()) {
        String voucherNumber = reply.result().getLong(0).toString();
        log.debug("Retrieved voucher number: {}", voucherNumber);
        SequenceNumber sequenceNumber = new SequenceNumber().withSequenceNumber(voucherNumber);
        asyncResultHandler.handle(succeededFuture(
            VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond200WithApplicationJson(sequenceNumber)));
      } else {
        String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
        asyncResultHandler
          .handle(succeededFuture(VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain(msg)));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
      asyncResultHandler
        .handle(succeededFuture(VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain(msg)));
    }
  }
}
