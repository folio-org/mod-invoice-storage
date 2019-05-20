package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.resource.VoucherStorageVoucherNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.Map;


public class VoucherNumberImpl implements VoucherStorageVoucherNumber {

  private final Messages messages = Messages.getInstance();

  private static final String VOUCHER_NUMBER_QUERY = "SELECT nextval('voucher_number')";
  public static final String CURRENT_VOUCHER_NUMBER_QUERY = "SELECT currval('voucher_number')";

  @Override
  public void getVoucherStorageVoucherNumber(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    VoucherNumberHelper voucherNumberHelper1 = new VoucherNumberHelper(okapiHeaders);
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .selectSingle(VOUCHER_NUMBER_QUERY, reply -> {
        voucherNumberHelper1.retrieveVoucherNumber(reply, asyncResultHandler, messages, lang, okapiHeaders, vertxContext);
      });
  }

  @Override
  public void getVoucherStorageVoucherNumberStart(String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    VoucherNumberHelper voucherNumberHelper = new VoucherNumberHelper(okapiHeaders);
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
    PostgresClient.getInstance(vertxContext.owner(), tenantId)
      .selectSingle(CURRENT_VOUCHER_NUMBER_QUERY, reply -> {
        voucherNumberHelper.retrieveVoucherNumber(reply, asyncResultHandler, messages, lang, okapiHeaders, vertxContext);
      });
  }
}
