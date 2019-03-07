package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.resource.InvoiceStorage;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.vertx.core.Future.succeededFuture;

public class InvoiceStorageImpl implements InvoiceStorage {

  public static final String NOT_SUPPORTED = "Not supported";

  @Validate
  @Override
  public void getInvoiceStorageInvoices(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoices(String lang, Invoice entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void putInvoiceStorageInvoicesById(String id, String lang, Invoice entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLines(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoiceLines(String lang, InvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void putInvoiceStorageInvoiceLinesById(String id, String lang, InvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLinesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoiceLinesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }


  @Override
  public void getInvoiceStorageInvoiceNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }

  @Override
  public void getInvoiceStorageInvoiceLineNumber(String invoiceId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }
}
