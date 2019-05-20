package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond200WithApplicationJson;
import static org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.tools.messages.Messages;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
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
  
  public void retrieveVoucherNumber(AsyncResult<JsonArray> reply, Handler<AsyncResult<Response>> asyncResultHandler,
      Messages messages, String lang, Map<String, String> okapiHeaders, Context vertxContext) {
    try {
      if (reply.succeeded()) {
        String voucherNumber = reply.result()
          .getList()
          .get(0)
          .toString();
        log.debug("Retrieved voucher number: {}", voucherNumber);
        asyncResultHandler
          .handle(succeededFuture(respond200WithApplicationJson(new SequenceNumber().withSequenceNumber(voucherNumber))));
      } else {
        throw new Exception(reply.cause());
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      asyncResultHandler
        .handle(succeededFuture(respond500WithTextPlain(messages.getMessage(lang, MessageConsts.InternalServerError))));
    }
  }
}
