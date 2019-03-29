package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.SequenceNumber;
import org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceNumber.GetInvoiceStorageInvoiceNumberResponse.*;

public class InvoiceNumberImpl implements InvoiceStorageInvoiceNumber {

  private final Messages messages = Messages.getInstance();

  private static final Logger log = LoggerFactory.getLogger(InvoiceNumberImpl.class);
  private static final String INVOICE_NUMBER_QUERY = "SELECT nextval('invoice_number')";

  @Override
  public void getInvoiceStorageInvoiceNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      try {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient.getInstance(vertxContext.owner(), tenantId).select(INVOICE_NUMBER_QUERY, reply -> {
          try {
            if (reply.succeeded()) {
              String invoiceNumber = reply.result().getResults().get(0).getList().get(0).toString();
              asyncResultHandler.handle(succeededFuture(respond200WithApplicationJson(new SequenceNumber().withSequenceNumber(invoiceNumber))));
            } else {
              Throwable cause = reply.cause();
              log.error(cause.getMessage(), cause);
              asyncResultHandler.handle(succeededFuture(respond400WithTextPlain(cause.getMessage())));
            }
          } catch (Exception e) {
            log.error(e.getMessage(), e);
            asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(messages.getMessage(lang, MessageConsts.InternalServerError))));
          }
        });
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        String message = messages.getMessage(lang, MessageConsts.InternalServerError);
        asyncResultHandler.handle(succeededFuture(respond500WithTextPlain(message)));
      }
    });
  }
}
