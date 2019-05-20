package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
import static org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond500WithTextPlain;
import static org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber.GetVoucherStorageVoucherNumberResponse.respond200WithApplicationJson;


public class VoucherNumberImpl implements VoucherStorageVoucherNumber {

  private final Messages messages = Messages.getInstance();

  private static final Logger log = LoggerFactory.getLogger(VoucherNumberImpl.class);
  private static final String VOUCHER_NUMBER_QUERY = "SELECT nextval('voucher_number')";
  private static final String CURRENT_VOUCHER_NUMBER_QUERY = "SELECT currval('voucher_number')";

  @Override
  public void getVoucherStorageVoucherNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId).selectSingle(VOUCHER_NUMBER_QUERY, reply -> {
        try {
          if(reply.succeeded()) {
            String voucherNumber = reply.result().getList().get(0).toString();
            log.debug("Retrieved voucher number: {}", voucherNumber);
            asyncResultHandler.handle(succeededFuture(respond200WithApplicationJson(new SequenceNumber().withSequenceNumber(voucherNumber))));
          } else {
            throw new Exception(reply.cause());
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
      });
    });
  }

  @Override
  public void getVoucherStorageVoucherNumberStart(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId).selectSingle(CURRENT_VOUCHER_NUMBER_QUERY, reply -> {
        try {
          if(reply.succeeded()) {
            String voucherNumber = reply.result().getList().get(0).toString();
            log.debug("Retrieved current voucher number: {}", voucherNumber);
            asyncResultHandler.handle(succeededFuture(respond200WithApplicationJson(new SequenceNumber().withSequenceNumber(voucherNumber))));
          } else {
            throw new Exception(reply.cause());
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
      });
    });
  }
}
