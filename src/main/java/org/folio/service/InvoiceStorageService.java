package org.folio.service;

import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_LOCATION;
import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_ID_FIELD_NAME;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_LINE_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_PREFIX;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import org.folio.rest.jaxrs.resource.InvoiceStorage.GetInvoiceStorageInvoiceLinesResponse;
import org.folio.rest.jaxrs.resource.InvoiceStorage.GetInvoiceStorageInvoicesDocumentsByIdResponse;
import org.folio.rest.jaxrs.resource.InvoiceStorage.GetInvoiceStorageInvoicesResponse;
import org.folio.rest.jaxrs.resource.InvoiceStorage.PutInvoiceStorageInvoicesByIdResponse;
import static org.folio.rest.persist.HelperUtils.combineCqlExpressions;
import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;
import static org.folio.rest.util.ResponseUtils.buildContentResponse;
import static org.folio.rest.util.ResponseUtils.buildErrorResponse;
import static org.folio.rest.util.ResponseUtils.buildNoContentResponse;
import static org.folio.rest.util.ResponseUtils.buildResponseWithLocation;
import static org.folio.rest.util.RestConstants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.folio.dao.invoice.InvoiceDAO;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.persist.*;

import io.vertx.core.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.impl.HttpStatusException;

public class InvoiceStorageService {

  private static final Logger log = LoggerFactory.getLogger(InvoiceStorageService.class);
  private static final String INVOICE_ID_MISMATCH_ERROR_MESSAGE = "Invoice id mismatch";

  final private InvoiceDAO invoiceDAO;

  public InvoiceStorageService(InvoiceDAO invoiceDAO) {
    this.invoiceDAO = invoiceDAO;
  }

  public void getInvoiceStorageInvoices(int offset, int limit, String query, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      EntitiesMetadataHolder<Invoice, InvoiceCollection> entitiesMetadataHolder =
        new EntitiesMetadataHolder<>(Invoice.class, InvoiceCollection.class, GetInvoiceStorageInvoicesResponse.class);
      QueryHolder cql = new QueryHolder(INVOICE_TABLE, query, offset, limit, lang);
      getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
    });
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
        new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
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
          .compose(t -> invoiceDAO.deleteInvoice(id, client))
          .compose(t -> client.endTx())
          .onComplete(result -> {
            if (result.failed()) {
              HttpStatusException cause = (HttpStatusException) result.cause();
              log.error("Invoice {} and associated lines if any were failed to be deleted", cause, id);
              // The result of rollback operation is not so important, main failure cause is used to build the response
              client.rollbackTransaction().onComplete(res -> asyncResultHandler.handle(buildErrorResponse(cause)));
            } else {
              log.info("Invoice {} and associated lines were successfully deleted", id);
              asyncResultHandler.handle(buildNoContentResponse());
            }
          });
      });
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      asyncResultHandler.handle(buildErrorResponse(
        new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
      ));
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

  public void getInvoiceStorageInvoicesDocumentsById(String id, int offset, int limit, String query, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String getByIdQuery = INVOICE_ID_FIELD_NAME + "==" + id;
    String resultQuery = StringUtils.isNotEmpty(query) ?
      combineCqlExpressions("and", getByIdQuery, query) : getByIdQuery;

    EntitiesMetadataHolder<Document, DocumentCollection> entitiesMetadataHolder =
      new EntitiesMetadataHolder<>(Document.class, DocumentCollection.class,
        GetInvoiceStorageInvoicesDocumentsByIdResponse.class);
    QueryHolder cql = new QueryHolder(DOCUMENT_TABLE, resultQuery, offset, limit, lang);
    getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
  }

  public void postInvoiceStorageInvoicesDocumentsById(String invoiceId, InvoiceDocument invoiceDoc,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      if (!StringUtils.equals(invoiceDoc.getDocumentMetadata().getInvoiceId(), invoiceId)) {
        asyncResultHandler.handle(buildErrorResponse(new HttpStatusException(
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

  public void getInvoiceStorageInvoiceLines(int offset, int limit, String query, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      EntitiesMetadataHolder<InvoiceLine, InvoiceLineCollection> entitiesMetadataHolder =
        new EntitiesMetadataHolder<>(InvoiceLine.class, InvoiceLineCollection.class,
          GetInvoiceStorageInvoiceLinesResponse.class);
      QueryHolder cql = new QueryHolder(INVOICE_LINE_TABLE, query, offset, limit, lang);
      getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
    });
  }

}
