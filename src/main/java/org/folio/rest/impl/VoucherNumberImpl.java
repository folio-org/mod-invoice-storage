package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.math.NumberUtils;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;


public class VoucherNumberImpl implements VoucherStorageVoucherNumber {

  private final Messages messages = Messages.getInstance();

  private static final Logger log = LoggerFactory.getLogger(VoucherNumberImpl.class);
  private static final String VOUCHER_NUMBER_QUERY = "SELECT nextval('voucher_number')";
  private static final String SET_START_SEQUENCE_VALUE_QUERY = "ALTER SEQUENCE voucher_number START WITH %s RESTART;";

  @Override
  public void getVoucherStorageVoucherNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId).selectSingle(VOUCHER_NUMBER_QUERY, reply -> {
        try {
          if(reply.succeeded()) {
            String voucherNumber = reply.result().getList().get(0).toString();
            log.debug("Retrieved voucher number: {}", voucherNumber);
            SequenceNumber sequenceNumber = new SequenceNumber().withSequenceNumber(voucherNumber);
            asyncResultHandler.handle(succeededFuture(VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond200WithApplicationJson(sequenceNumber)));
          } else {
            throw new Exception(reply.cause());
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
          asyncResultHandler.handle(succeededFuture(VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain(msg)));
        }
      });
    });
  }

  @Override
  public void postVoucherStorageVoucherNumberStartByValue(String value, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      if(NumberUtils.isDigits(value)) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient.getInstance(vertxContext.owner(), tenantId).execute(String.format(SET_START_SEQUENCE_VALUE_QUERY, value), reply -> {
          try {
            if(reply.succeeded()) {
              log.debug("(Re)set start value for voucher number sequence: {}", value);
              asyncResultHandler.handle(succeededFuture(VoucherStorageVoucherNumber.PostVoucherStorageVoucherNumberStartByValueResponse.respond204()));
            } else {
              throw new Exception(reply.cause());
            }
          } catch (Exception e) {
            log.error(e.getMessage(), e);
            String msg = messages.getMessage(lang, MessageConsts.InternalServerError);
            asyncResultHandler.handle(succeededFuture(VoucherStorageVoucherNumber.PostVoucherStorageVoucherNumberStartByValueResponse.respond500WithTextPlain(msg)));
          }
        });
      } else {
        log.debug("Illegal value: {}", value);
        asyncResultHandler.handle(succeededFuture(VoucherStorageVoucherNumber.PostVoucherStorageVoucherNumberStartByValueResponse.respond400WithTextPlain("Bad request - illegal value")));
      }
    });
  }
}
