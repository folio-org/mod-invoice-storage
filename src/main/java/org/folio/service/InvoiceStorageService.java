package org.folio.service;

import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_LOCATION;
import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_ID_FIELD_NAME;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_PREFIX;
import static org.folio.rest.utils.HelperUtils.combineCqlExpressions;
import static org.folio.rest.utils.ResponseUtils.buildBadRequestResponse;
import static org.folio.rest.utils.ResponseUtils.buildOkResponse;
import static org.folio.rest.utils.ResponseUtils.buildErrorResponse;
import static org.folio.rest.utils.ResponseUtils.buildNoContentResponse;
import static org.folio.rest.utils.ResponseUtils.buildResponseWithLocation;
import static org.folio.rest.utils.RestConstants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.dao.invoice.InvoiceDAO;
import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.jaxrs.model.DocumentCollection;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.jaxrs.resource.InvoiceStorage.GetInvoiceStorageInvoicesDocumentsByIdResponse;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PgUtil;
import org.folio.service.audit.AuditOutboxService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.HttpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class InvoiceStorageService {

  private static final String INVOICE_ID_MISMATCH_ERROR_MESSAGE = "Invoice id mismatch";

  private final InvoiceDAO invoiceDAO;
  private final AuditOutboxService auditOutboxService;

  public void postInvoiceStorageInvoices(Invoice invoice, Handler<AsyncResult<Response>> asyncResultHandler,
                                         Context vertxContext, Map<String, String> headers) {
    log.info("postInvoiceStorageInvoices:: Creating a new invoice by id: {}", invoice.getId());
    new DBClient(vertxContext, headers).getPgClient()
      .withTrans(conn -> invoiceDAO.createInvoice(invoice, conn)
        .compose(invoiceId -> auditOutboxService.saveInvoiceOutboxLog(conn, invoice, InvoiceAuditEvent.Action.CREATE, headers)))
      .onSuccess(s -> {
        log.info("postInvoiceStorageInvoices:: Successfully created a new invoice by id: {}", invoice.getId());
        auditOutboxService.processOutboxEventLogs(headers, vertxContext);
        asyncResultHandler.handle(buildResponseWithLocation(headers.get(OKAPI_URL), INVOICE_PREFIX + invoice.getId(), invoice));
      })
      .onFailure(f -> {
        log.error("Error occurred while creating a new invoice with id: {}", invoice.getId(), f);
        asyncResultHandler.handle(buildErrorResponse(f));
      });
  }

  public void putInvoiceStorageInvoicesById(String id, Invoice invoice, Map<String, String> headers,
                                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.info("putInvoiceStorageInvoicesById:: Updating invoice with id: {}", id);
    if (StringUtils.isBlank(id)) {
      asyncResultHandler.handle(buildBadRequestResponse("Invoice id is required"));
    }
    new DBClient(vertxContext, headers).getPgClient()
      .withTrans(conn -> invoiceDAO.updateInvoice(id, invoice, conn)
        .compose(invoiceId -> auditOutboxService.saveInvoiceOutboxLog(conn, invoice, InvoiceAuditEvent.Action.EDIT, headers)))
      .onSuccess(s -> {
        log.info("putInvoiceStorageInvoicesById:: Successfully updated invoice with id: {}", id);
        auditOutboxService.processOutboxEventLogs(headers, vertxContext);
        asyncResultHandler.handle(buildNoContentResponse());
      })
      .onFailure(f -> {
        log.error("Error occurred while updating invoice with id: {}", id, f);
        asyncResultHandler.handle(buildErrorResponse(f));
      });
  }

  public void deleteInvoiceStorageInvoicesById(String id, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext, Map<String, String> headers) {
    try {
      vertxContext.runOnContext(v -> {
        log.info("deleteInvoiceStorageInvoicesById:: Delete invoice {}", id);

        DBClient client = new DBClient(vertxContext, headers);
        client.startTx()
          .compose(t -> invoiceDAO.deleteInvoiceLinesByInvoiceId(id, client))
          .compose(t -> invoiceDAO.deleteInvoiceDocumentsByInvoiceId(id, client))
          .compose(t -> invoiceDAO.deleteInvoice(id, client))
          .compose(t -> client.endTx())
          .onComplete(result -> {
            if (result.failed()) {
              HttpException cause = (HttpException) result.cause();
              log.error("Invoice '{}' and associated lines and documents if any failed to be deleted", id, cause);
              // The result of rollback operation is not so important, main failure cause is used to build the response
              client.rollbackTransaction().onComplete(res -> asyncResultHandler.handle(buildErrorResponse(cause)));
            } else {
              log.info("deleteInvoiceStorageInvoicesById:: Invoice {} and associated lines and documents if any were successfully deleted", id);
              asyncResultHandler.handle(buildNoContentResponse());
            }
          });
      });
    } catch (Exception e) {
      log.error("Error occurred while deleting invoice with id: {}", id, e);
      asyncResultHandler.handle(buildErrorResponse(new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  public void getInvoiceStorageInvoicesDocumentsById(String id, int offset, int limit, String query,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("getInvoiceStorageInvoicesDocumentsById:: Getting invoice documents by invoice id: {}", id);
    String getByIdQuery = INVOICE_ID_FIELD_NAME + "==" + id;
    String resultQuery = StringUtils.isNotEmpty(query) ? combineCqlExpressions("and", getByIdQuery, query) : getByIdQuery;

    PgUtil.get(DOCUMENT_TABLE, Document.class, DocumentCollection.class, resultQuery, offset, limit, okapiHeaders, vertxContext,
      GetInvoiceStorageInvoicesDocumentsByIdResponse.class, asyncResultHandler);
    log.info("getInvoiceStorageInvoicesDocumentsById:: Invoice documents by invoice id: {} were successfully retrieved", id);
  }

  public void postInvoiceStorageInvoicesDocumentsById(String invoiceId, InvoiceDocument invoiceDoc,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("postInvoiceStorageInvoicesDocumentsById:: Creating a new invoice document for invoice with id: {}", invoiceId);
    vertxContext.runOnContext((Void v) -> {
      String invoiceIdFromPath = invoiceDoc.getDocumentMetadata().getInvoiceId();
      if (!StringUtils.equals(invoiceIdFromPath, invoiceId)) {
        log.error("postInvoiceStorageInvoicesDocumentsById:: Invoice id mismatch. Invoice id from path: {}, invoice id from request body: {}", invoiceId, invoiceIdFromPath);
        asyncResultHandler.handle(buildErrorResponse(new HttpException(
          Response.Status.BAD_REQUEST.getStatusCode(), INVOICE_ID_MISMATCH_ERROR_MESSAGE
        )));
        return;
      }
      DBClient client = new DBClient(vertxContext, okapiHeaders);
      invoiceDAO.createInvoiceDocument(invoiceDoc, client)
        .onComplete(reply -> {
          if (reply.succeeded()) {
            log.info("postInvoiceStorageInvoicesDocumentsById:: Successfully created a new invoice document for invoice with id: {}", invoiceId);
            asyncResultHandler.handle(buildResponseWithLocation(okapiHeaders.get(OKAPI_URL),
                String.format(DOCUMENT_LOCATION, invoiceId, invoiceDoc.getDocumentMetadata().getId()), invoiceDoc));
          } else {
            log.error("Error occurred while creating a new invoice document for invoice with id: {}", invoiceId, reply.cause());
            asyncResultHandler.handle(buildErrorResponse(reply.cause()));
          }
        });
    });
  }

  public void getInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String invoiceId, String documentId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("getInvoiceStorageInvoicesDocumentsByIdAndDocumentId:: Getting invoice document with id: {} for invoice with id: {}", documentId, invoiceId);
    vertxContext.runOnContext((Void v) -> {
      DBClient client = new DBClient(vertxContext, okapiHeaders);
      invoiceDAO.getInvoiceDocument(invoiceId, documentId, client)
        .onComplete(reply -> {
          if (reply.succeeded()) {
            log.info("getInvoiceStorageInvoicesDocumentsByIdAndDocumentId:: Successfully retrieved invoice document with id: {} for invoice with id: {}", documentId, invoiceId);
            asyncResultHandler.handle(buildOkResponse(reply.result()));
          } else {
            log.error("Error occurred while retrieving invoice document with id: {} for invoice with id: {}", documentId, invoiceId, reply.cause());
            asyncResultHandler.handle(buildErrorResponse(reply.cause()));
          }
        });
    });
  }

}
