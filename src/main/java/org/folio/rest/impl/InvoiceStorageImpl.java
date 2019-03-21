package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceCollection;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.resource.InvoiceStorage;
import org.folio.rest.persist.EntitiesMetadataHolder;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.QueryHolder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class InvoiceStorageImpl implements InvoiceStorage {

	private static final String INVOICE_TABLE = "invoice";
//	private static final String INVOICE_LINES_TABLE = "invoice_line";
	
  private String idFieldName = "id";


  public InvoiceStorageImpl(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx, tenantId).setIdField(idFieldName);
  }
  
  @Validate
  @Override
  public void getInvoiceStorageInvoices(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      EntitiesMetadataHolder<Invoice, InvoiceCollection> entitiesMetadataHolder = new EntitiesMetadataHolder<>(Invoice.class, InvoiceCollection.class, GetInvoiceStorageInvoicesResponse.class);
      QueryHolder cql = new QueryHolder(INVOICE_TABLE, query, offset, limit, lang);
      getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
    });
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoices(String lang, Invoice entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
  	PgUtil.post(INVOICE_TABLE, entity, okapiHeaders, vertxContext, PostInvoiceStorageInvoicesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
  	PgUtil.getById(INVOICE_TABLE, Invoice.class, id, okapiHeaders,vertxContext, GetInvoiceStorageInvoicesByIdResponse.class, asyncResultHandler);
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
    vertxContext.runOnContext((Void v) -> {
//      EntitiesMetadataHolder<InvoiceLine, InvoiceLineCollection> entitiesMetadataHolder = new EntitiesMetadataHolder<>(InvoiceLine.class, InvoiceLineCollection.class, GetInvoiceStorageInvoiceLinesResponse.class);
//      QueryHolder cql = new QueryHolder(INVOICE_LINES_TABLE, query, offset, limit, lang);
//      getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
    });
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoiceLines(String lang, InvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
  	//PgUtil.post(INVOICE_LINES_TABLE, entity, okapiHeaders, vertxContext, PostInvoiceStorageInvoiceLinesResponse.class, asyncResultHandler);
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
