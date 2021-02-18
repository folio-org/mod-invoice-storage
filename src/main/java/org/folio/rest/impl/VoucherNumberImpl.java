package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;


public class VoucherNumberImpl implements VoucherStorageVoucherNumber {

  private final Messages messages = Messages.getInstance();

  private static final Logger log = LogManager.getLogger(VoucherNumberImpl.class);
  private static final String VOUCHER_NUMBER_QUERY = "SELECT nextval('voucher_number')";
  private static final String SET_START_SEQUENCE_VALUE_QUERY = "ALTER SEQUENCE voucher_number START WITH %s RESTART;";
  public static final String CURRENT_VOUCHER_NUMBER_QUERY = "SELECT pg_sequences.start_value FROM pg_sequences WHERE sequencename = 'voucher_number'";

  @Override
  public void getVoucherStorageVoucherNumber(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    getVoucherNumber(lang, okapiHeaders, asyncResultHandler, vertxContext, VOUCHER_NUMBER_QUERY);
  }

  @Override
  public void postVoucherStorageVoucherNumberStartByValue(String value, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      if (NumberUtils.isDigits(value)) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient.getInstance(vertxContext.owner(), tenantId)
          .execute(String.format(SET_START_SEQUENCE_VALUE_QUERY, value), reply -> {
            if (reply.succeeded()) {
              log.debug("(Re)set start value for voucher number sequence: {}", value);
              asyncResultHandler.handle(
                  succeededFuture(VoucherStorageVoucherNumber.PostVoucherStorageVoucherNumberStartByValueResponse.respond204()));
            } else {
              log.error(reply.cause()
                .getMessage(), reply.cause());
              String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
              asyncResultHandler.handle(succeededFuture(
                  VoucherStorageVoucherNumber.PostVoucherStorageVoucherNumberStartByValueResponse.respond500WithTextPlain(msg)));
            }
          });
      } else {
        log.debug("Illegal value: {}", value);
        asyncResultHandler.handle(succeededFuture(VoucherStorageVoucherNumber.PostVoucherStorageVoucherNumberStartByValueResponse
          .respond400WithTextPlain("Bad request - illegal value")));
      }
    });
  }

  @Override
  public void getVoucherStorageVoucherNumberStart(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug(" === Retrieving current start value for a voucher number sequence === ");
    getVoucherNumber(lang, okapiHeaders, asyncResultHandler, vertxContext, CURRENT_VOUCHER_NUMBER_QUERY);
  }

  private void getVoucherNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext, String voucherNumberQuery) {
    vertxContext.runOnContext((Void v) -> {
      VoucherNumberHelper getVoucherNumberStartHelper = new VoucherNumberHelper();
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .selectSingle(voucherNumberQuery,
            reply -> getVoucherNumberStartHelper.retrieveVoucherNumber(reply, asyncResultHandler, messages, lang));
    });
  }
}
