package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.model.PoLineInvoiceLine;
import org.folio.rest.jaxrs.model.PoLineInvoiceLineCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorage;
import org.folio.rest.jaxrs.resource.InvoiceStoragePoLineNumber;
import org.folio.rest.persist.PgUtil;

import javax.ws.rs.core.Response;
import java.util.Map;

public class InvoiceStoragePoLineNumberImpl implements InvoiceStoragePoLineNumber {
  public static final String PO_LINE_INVOICE_LINE_TABLE = "po_line_vs_invoice_lines";

  @Override
  public void getInvoiceStoragePoLineNumber(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    System.out.print("inside : getInvoiceStoragePoLineNumber");

    PgUtil.get(PO_LINE_INVOICE_LINE_TABLE, PoLineInvoiceLine.class, PoLineInvoiceLineCollection.class, query, offset, limit, okapiHeaders, vertxContext,
      InvoiceStoragePoLineNumber.GetInvoiceStoragePoLineNumberResponse.class, asyncResultHandler);
  }

  @Override
  public void postInvoiceStoragePoLineNumber(String lang, PoLineInvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
  //invoiceStorageService.postInvoiceStorageInvoices(entity, asyncResultHandler, vertxContext, okapiHeaders);
    System.out.print("inside : postInvoiceStoragePoLineNumber");

    PgUtil.post(PO_LINE_INVOICE_LINE_TABLE, entity, okapiHeaders, vertxContext, InvoiceStoragePoLineNumber.PostInvoiceStoragePoLineNumberResponse.class,
      asyncResultHandler);
  }

  @Override
  public void getInvoiceStoragePoLineNumberById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(PO_LINE_INVOICE_LINE_TABLE, PoLineInvoiceLine.class, id, okapiHeaders, vertxContext,
      InvoiceStoragePoLineNumber.GetInvoiceStoragePoLineNumberByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteInvoiceStoragePoLineNumberById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(PO_LINE_INVOICE_LINE_TABLE, id, okapiHeaders, vertxContext, InvoiceStoragePoLineNumber.DeleteInvoiceStoragePoLineNumberByIdResponse.class,
      asyncResultHandler);
  }

  @Override
  public void putInvoiceStoragePoLineNumberById(String id, String lang, PoLineInvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(PO_LINE_INVOICE_LINE_TABLE, entity, id, okapiHeaders, vertxContext, InvoiceStorage.PutInvoiceStorageInvoiceLinesByIdResponse.class,
      asyncResultHandler);
  }
}
