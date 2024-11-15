package org.folio.rest.impl;

import static org.folio.rest.utils.ResponseUtils.buildErrorResponse;
import static org.folio.rest.utils.ResponseUtils.buildNoContentResponse;
import static org.folio.rest.utils.ResponseUtils.buildOkResponse;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.ext.web.handler.HttpException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;


public class VoucherNumberImpl implements VoucherStorageVoucherNumber {

  private static final Logger log = LogManager.getLogger(VoucherNumberImpl.class);
  private static final String VOUCHER_NUMBER_QUERY = "SELECT nextval('voucher_number')";
  private static final String SET_START_SEQUENCE_VALUE_QUERY = "ALTER SEQUENCE voucher_number START WITH %s RESTART;";
  public static final String CURRENT_VOUCHER_NUMBER_QUERY = "SELECT pg_sequences.start_value FROM pg_sequences " +
    "WHERE sequencename = 'voucher_number' AND sequenceowner = '%_mod_invoice_storage'";

  @Override
  public void getVoucherStorageVoucherNumber(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    getVoucherNumber(okapiHeaders, asyncResultHandler, vertxContext, VOUCHER_NUMBER_QUERY);
  }

  @Override
  public void postVoucherStorageVoucherNumberStartByValue(String value, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("Trying to set value '{}' for a voucher number sequence", value);
    vertxContext.runOnContext((Void v) -> {
      if (NumberUtils.isDigits(value)) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient.getInstance(vertxContext.owner(), tenantId)
          .execute(String.format(SET_START_SEQUENCE_VALUE_QUERY, value), reply -> {
            if (reply.succeeded()) {
              log.debug("(Re)set start value for voucher number sequence: {}", value);
              asyncResultHandler.handle(buildNoContentResponse());
            } else {
              log.error("Failed to (re)set start value for voucher number sequence: {}", value);
              asyncResultHandler.handle(buildErrorResponse(reply.cause()));
            }
          });
      } else {
        log.error("Error while trying to set start value for voucher number sequence: {}", value);
        asyncResultHandler.handle(buildErrorResponse(new HttpException(Response.Status.BAD_REQUEST.getStatusCode(), "Bad request - illegal value")));
      }
    });
  }

  @Override
  public void getVoucherStorageVoucherNumberStart(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("Trying to retrieving current start value for a voucher number sequence");
    getVoucherNumber(okapiHeaders, asyncResultHandler, vertxContext, CURRENT_VOUCHER_NUMBER_QUERY);
  }

  private void getVoucherNumber(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext, String voucherNumberQuery) {
    log.debug("getVoucherNumber:: Getting voucher number by query: {}", voucherNumberQuery);
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId)
        .selectSingle(String.format(voucherNumberQuery, tenantId),
          reply -> {
            try {
              if (reply.succeeded()) {
                String voucherNumber = reply.result().getLong(0).toString();
                log.info("getVoucherNumber:: Retrieved voucher number: {}", voucherNumber);
                SequenceNumber sequenceNumber = new SequenceNumber().withSequenceNumber(voucherNumber);
                asyncResultHandler.handle(buildOkResponse(sequenceNumber));
              } else {
                log.error("getVoucherNumber:: Failed to retrieve voucher number", reply.cause());
                asyncResultHandler.handle(buildErrorResponse(reply.cause()));
              }
            } catch (Exception e) {
              log.error("Error while handling response for voucher number request", e);
              asyncResultHandler.handle(buildErrorResponse(e));
            }
          });
    });
    log.info("getVoucherNumber:: Finished getting voucher number by query: {}", voucherNumberQuery);
  }
}
