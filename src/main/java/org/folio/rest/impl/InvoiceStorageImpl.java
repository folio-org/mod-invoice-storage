package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceCollection;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorage;
import org.folio.rest.persist.PgUtil;
import org.folio.service.InvoiceStorageService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class InvoiceStorageImpl implements InvoiceStorage {

  public static final String DOCUMENT_LOCATION = "/invoice-storage/invoices/%s/documents/%s";
  public static final String DOCUMENT_TABLE = "documents";
  public static final String INVOICE_ID_FIELD_NAME = "invoiceId";
  public static final String INVOICE_LINE_TABLE = "invoice_lines";
  public static final String INVOICE_PREFIX = "/invoice-storage/invoices/";
  public static final String INVOICE_TABLE = "invoices";

  @Autowired
  private InvoiceStorageService invoiceStorageService;

  public InvoiceStorageImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoices(String totalRecords, int offset, int limit, String query, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(INVOICE_TABLE, Invoice.class, InvoiceCollection.class, query, offset, limit, okapiHeaders, vertxContext,
      GetInvoiceStorageInvoicesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoices(Invoice entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceStorageService.postInvoiceStorageInvoices(entity, asyncResultHandler, vertxContext, okapiHeaders);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INVOICE_TABLE, Invoice.class, id, okapiHeaders, vertxContext, GetInvoiceStorageInvoicesByIdResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceStorageService.deleteInvoiceStorageInvoicesById(id, asyncResultHandler, vertxContext, okapiHeaders);
  }

  @Validate
  @Override
  public void putInvoiceStorageInvoicesById(String id, Invoice entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceStorageService.putInvoiceStorageInvoicesById(id, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesDocumentsById(String id, String totalRecords, int offset, int limit, String query,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceStorageService.getInvoiceStorageInvoicesDocumentsById(id, offset, limit, query, okapiHeaders, asyncResultHandler,
        vertxContext);
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoicesDocumentsById(String id, InvoiceDocument entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceStorageService.postInvoiceStorageInvoicesDocumentsById(id, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String id, String documentId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceStorageService.getInvoiceStorageInvoicesDocumentsByIdAndDocumentId(id, documentId, okapiHeaders, asyncResultHandler,
        vertxContext);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String id, String documentId,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(DOCUMENT_TABLE, documentId, okapiHeaders, vertxContext,
        DeleteInvoiceStorageInvoicesDocumentsByIdAndDocumentIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLines(String totalRecords, int offset, int limit, String query, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(INVOICE_LINE_TABLE, InvoiceLine.class, InvoiceLineCollection.class, query, offset, limit, okapiHeaders, vertxContext,
        GetInvoiceStorageInvoiceLinesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoiceLines(InvoiceLine entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(INVOICE_LINE_TABLE, entity, okapiHeaders, vertxContext, PostInvoiceStorageInvoiceLinesResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void putInvoiceStorageInvoiceLinesById(String id, InvoiceLine entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(INVOICE_LINE_TABLE, entity, id, okapiHeaders, vertxContext, PutInvoiceStorageInvoiceLinesByIdResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLinesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INVOICE_LINE_TABLE, InvoiceLine.class, id, okapiHeaders, vertxContext,
        GetInvoiceStorageInvoiceLinesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoiceLinesById(String id, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(INVOICE_LINE_TABLE, id, okapiHeaders, vertxContext, DeleteInvoiceStorageInvoiceLinesByIdResponse.class,
        asyncResultHandler);
  }
}
