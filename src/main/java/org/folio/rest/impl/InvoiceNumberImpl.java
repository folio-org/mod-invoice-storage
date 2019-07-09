package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceNumber.GetInvoiceStorageInvoiceNumberResponse.respond200WithApplicationJson;
import static org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceNumber.GetInvoiceStorageInvoiceNumberResponse.respond500WithTextPlain;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class InvoiceNumberImpl implements InvoiceStorageInvoiceNumber {

  private final Messages messages = Messages.getInstance();

  private static final Logger log = LoggerFactory.getLogger(InvoiceNumberImpl.class);
  private static final String INVOICE_NUMBER_QUERY = "SELECT nextval('invoice_number')";

  @Override
  public void getInvoiceStorageInvoiceNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
      PostgresClient.getInstance(vertxContext.owner(), tenantId).selectSingle(INVOICE_NUMBER_QUERY, reply -> {
        try {
          if(reply.succeeded()) {
            String invoiceNumber = reply.result().getList().get(0).toString();
            log.debug("Retrieved invoice number: {}", invoiceNumber);
            asyncResultHandler.handle(succeededFuture(respond200WithApplicationJson(new SequenceNumber().withSequenceNumber(invoiceNumber))));
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
