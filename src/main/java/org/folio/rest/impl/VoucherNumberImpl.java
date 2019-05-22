package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.Map;


public class VoucherNumberImpl implements VoucherStorageVoucherNumber {

  private static final Logger log = LoggerFactory.getLogger(VoucherNumberImpl.class);
  private final Messages messages = Messages.getInstance();

  private static final String VOUCHER_NUMBER_QUERY = "SELECT nextval('voucher_number')";
  public static final String CURRENT_VOUCHER_NUMBER_QUERY = "SELECT pg_sequences.start_value FROM pg_sequences WHERE sequencename = 'voucher_number'";
  
  @Override
  public void getVoucherStorageVoucherNumber(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    VoucherNumberHelper getVoucherNumberHelper = new VoucherNumberHelper(okapiHeaders);
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .selectSingle(VOUCHER_NUMBER_QUERY,
          reply -> getVoucherNumberHelper.retrieveVoucherNumber(reply, asyncResultHandler, messages, lang));
  }

  @Override
  public void getVoucherStorageVoucherNumberStart(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug(" === Retrieving current start value for a voucher number sequence === ");
    VoucherNumberHelper getVoucherNumberStartHelper = new VoucherNumberHelper(okapiHeaders);
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .selectSingle(CURRENT_VOUCHER_NUMBER_QUERY,
          reply -> {
            getVoucherNumberStartHelper.retrieveVoucherNumber(reply, asyncResultHandler, messages, lang);
          });
  }
}
