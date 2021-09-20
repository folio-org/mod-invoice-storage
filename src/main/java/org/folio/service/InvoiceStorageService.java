package org.folio.service;

import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_LOCATION;
import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_ID_FIELD_NAME;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_PREFIX;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import static org.folio.rest.utils.HelperUtils.combineCqlExpressions;
import static org.folio.rest.utils.ResponseUtils.buildContentResponse;
import static org.folio.rest.utils.ResponseUtils.buildErrorResponse;
import static org.folio.rest.utils.ResponseUtils.buildNoContentResponse;
import static org.folio.rest.utils.ResponseUtils.buildResponseWithLocation;
import static org.folio.rest.utils.RestConstants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.invoice.InvoiceDAO;
import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.jaxrs.model.DocumentCollection;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.jaxrs.resource.InvoiceStorage.GetInvoiceStorageInvoicesDocumentsByIdResponse;
import org.folio.rest.jaxrs.resource.InvoiceStorage.PutInvoiceStorageInvoicesByIdResponse;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PgUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.HttpException;

public class InvoiceStorageService {

  private static final Logger log = LogManager.getLogger(InvoiceStorageService.class);
  private static final String INVOICE_ID_MISMATCH_ERROR_MESSAGE = "Invoice id mismatch";

  private final InvoiceDAO invoiceDAO;

  public InvoiceStorageService(InvoiceDAO invoiceDAO) {
    this.invoiceDAO = invoiceDAO;
  }

  public void postInvoiceStorageInvoices(Invoice invoice, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext, Map<String, String> headers) {
    try {
      vertxContext.runOnContext(v -> {
        log.info("Creating a new invoice");

        DBClient client = new DBClient(vertxContext, headers);
        client.startTx()
          .compose(t -> invoiceDAO.createInvoice(invoice, client))
          .compose(t -> invoiceDAO.createSequence(invoice.getId(), client))
          .compose(t -> client.endTx())
          .onComplete(reply -> {
            if (reply.failed()) {
              // The result of rollback operation is not so important, main failure cause is used to build the response
              client.rollbackTransaction().onComplete(res -> asyncResultHandler.handle(buildErrorResponse(
                reply.cause())));
            } else {
              log.info("Preparing response to client");
              asyncResultHandler.handle(
                buildResponseWithLocation(headers.get(OKAPI_URL), INVOICE_PREFIX + invoice.getId(), invoice)
              );
            }
          });
      });
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      asyncResultHandler.handle(buildErrorResponse(
        new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
      ));
    }
  }

  public void deleteInvoiceStorageInvoicesById(String id, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext, Map<String, String> headers) {
    try {
      vertxContext.runOnContext(v -> {
        log.info("Delete invoice {}", id);

        DBClient client = new DBClient(vertxContext, headers);
        client.startTx()
          .compose(t -> invoiceDAO.deleteInvoiceLinesByInvoiceId(id, client))
          .compose(t -> invoiceDAO.deleteSequenceByInvoiceId(id, client))
          .compose(t -> invoiceDAO.deleteInvoiceDocumentsByInvoiceId(id, client))
          .compose(t -> invoiceDAO.deleteInvoice(id, client))
          .compose(t -> client.endTx())
          .onComplete(result -> {
            if (result.failed()) {
              HttpException cause = (HttpException) result.cause();
              String errorMessage = String.format("Invoice %s and associated lines and documents if any failed to be deleted", id);
              log.error(errorMessage, cause);
              // The result of rollback operation is not so important, main failure cause is used to build the response
              client.rollbackTransaction().onComplete(res -> asyncResultHandler.handle(buildErrorResponse(cause)));
            } else {
              log.info("Invoice {} and associated lines and documents if any were successfully deleted", id);
              asyncResultHandler.handle(buildNoContentResponse());
            }
          });
      });
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      asyncResultHandler.handle(buildErrorResponse(new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  public void putInvoiceStorageInvoicesById(String id, Invoice invoice, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(INVOICE_TABLE, invoice, id, okapiHeaders, vertxContext,
      PutInvoiceStorageInvoicesByIdResponse.class, reply -> {
        asyncResultHandler.handle(reply);
        DBClient client = new DBClient(vertxContext, okapiHeaders);
        invoiceDAO.deleteSequence(invoice, client);
      });
  }

  public void getInvoiceStorageInvoicesDocumentsById(String id, int offset, int limit, String query,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String getByIdQuery = INVOICE_ID_FIELD_NAME + "==" + id;
    String resultQuery = StringUtils.isNotEmpty(query) ? combineCqlExpressions("and", getByIdQuery, query) : getByIdQuery;

    PgUtil.get(DOCUMENT_TABLE, Document.class, DocumentCollection.class, resultQuery, offset, limit, okapiHeaders, vertxContext,
      GetInvoiceStorageInvoicesDocumentsByIdResponse.class, asyncResultHandler);
  }

  public void postInvoiceStorageInvoicesDocumentsById(String invoiceId, InvoiceDocument invoiceDoc,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      if (!StringUtils.equals(invoiceDoc.getDocumentMetadata().getInvoiceId(), invoiceId)) {
        asyncResultHandler.handle(buildErrorResponse(new HttpException(
          Response.Status.BAD_REQUEST.getStatusCode(), INVOICE_ID_MISMATCH_ERROR_MESSAGE
        )));
        return;
      }
      DBClient client = new DBClient(vertxContext, okapiHeaders);
      invoiceDAO.createInvoiceDocument(invoiceDoc, client)
        .onComplete(reply -> {
          if (reply.succeeded()) {
            asyncResultHandler.handle(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL),
                String.format(DOCUMENT_LOCATION, invoiceId, invoiceDoc.getDocumentMetadata().getId()), invoiceDoc));
          } else {
            asyncResultHandler.handle(buildErrorResponse(reply.cause()));
          }
        });
    });
  }

  public void getInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String invoiceId, String documentId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      DBClient client = new DBClient(vertxContext, okapiHeaders);
      invoiceDAO.getInvoiceDocument(invoiceId, documentId, client)
        .onComplete(reply -> {
          if (reply.succeeded()) {
            asyncResultHandler.handle(buildContentResponse(reply.result()));
          } else {
            asyncResultHandler.handle(buildErrorResponse(reply.cause()));
          }
        });
    });
  }

}
