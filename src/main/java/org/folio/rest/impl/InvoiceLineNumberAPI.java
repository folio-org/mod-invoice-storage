package org.folio.rest.impl;

import static org.folio.rest.utils.HelperUtils.SequenceQuery.GET_IL_NUMBER_FROM_SEQUENCE;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.InvoiceLineNumber;
import org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceLineNumber;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class InvoiceLineNumberAPI implements InvoiceStorageInvoiceLineNumber {

  private static final Logger log = LogManager.getLogger(InvoiceLineNumberAPI.class);
  private final Messages messages = Messages.getInstance();

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLineNumber(String invoiceId, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("Trying to get invoice line number for invoiceId: {}", invoiceId);
    vertxContext.runOnContext((Void v) -> {
      try {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient.getInstance(vertxContext.owner(), tenantId)
          .selectSingle(GET_IL_NUMBER_FROM_SEQUENCE.getQuery(invoiceId), getILNumberReply -> {
            try {
              if (getILNumberReply.succeeded()) {
                String invoiceLineNumber = getILNumberReply.result()
                  .getLong(0)
                  .toString();
                log.info("Successfully retrieved invoice line number {} for invoiceId: {}", invoiceLineNumber, invoiceId);
                asyncResultHandler
                  .handle(Future.succeededFuture(InvoiceStorageInvoiceLineNumber.GetInvoiceStorageInvoiceLineNumberResponse
                    .respond200WithApplicationJson(new InvoiceLineNumber().withSequenceNumber(invoiceLineNumber))));
              } else {
                log.error("Failed to retrieve invoice line number for invoiceId: {}", invoiceId, getILNumberReply.cause());
                throw new Exception("Unable to generate invoice line number from sequence for invoiceId: " + invoiceId);              }
            } catch (Exception e) {
              log.error("Error while handling response for invoice line number request for invoiceId: {}", invoiceId, e);
              logErrorAndRespond400(asyncResultHandler, getILNumberReply.cause());
            }
          });
      } catch (Exception e) {
        log.error("Error while attempting to retrieve invoice line number for invoiceId: {}", invoiceId, e);
        logErrorAndRespond500(lang, asyncResultHandler, e);
      }
    });
	}

  private void logErrorAndRespond400(Handler<AsyncResult<Response>> asyncResultHandler, Throwable e) {
    log.error("Bad Request", e);
    asyncResultHandler.handle(Future.succeededFuture(GetInvoiceStorageInvoiceLineNumberResponse
      .respond400WithTextPlain(e.getMessage())));
  }

  private void logErrorAndRespond500(String lang, Handler<AsyncResult<Response>> asyncResultHandler, Throwable e) {
    log.error("Internal Server Error", e);
    asyncResultHandler.handle(Future.succeededFuture(GetInvoiceStorageInvoiceLineNumberResponse
      .respond500WithTextPlain(messages.getMessage(lang, MessageConsts.InternalServerError))));
  }
}
