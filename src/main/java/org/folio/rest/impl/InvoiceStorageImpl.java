package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;
import static org.folio.rest.persist.HelperUtils.SequenceQuery.CREATE_SEQUENCE;
import static org.folio.rest.persist.HelperUtils.SequenceQuery.DROP_SEQUENCE;

import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceCollection;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorage;
import org.folio.rest.persist.EntitiesMetadataHolder;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.QueryHolder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.handler.impl.HttpStatusException;

public class InvoiceStorageImpl implements InvoiceStorage {

private static final Logger log = LoggerFactory.getLogger(InvoiceLineNumberAPI.class);

private static final String INVOICE_TABLE = "invoice";
private static final String INVOICE_LINE_TABLE = "invoice_line";
private static final String INVOICE_PREFIX = "/invoice-storage/invoices/";

private PostgresClient pgClient;

private String idFieldName = "id";

  public InvoiceStorageImpl(Vertx vertx, String tenantId) {
    pgClient = PostgresClient.getInstance(vertx, tenantId);
    pgClient.setIdField(idFieldName);
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
    try {
      vertxContext.runOnContext(v -> {
      	log.debug("Creating a new invoice");
        Future.succeededFuture(new Tx<>(entity))
          .compose(this::startTx)
          .compose(this::createInvoice)
          .compose(this::createSequence)
          .compose(this::endTx)
          .setHandler(reply -> {
            if (reply.failed()) {
              HttpStatusException cause = (HttpStatusException) reply.cause();
              if(cause.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond400WithTextPlain(cause.getPayload())));
              } else if(cause.getStatusCode() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond401WithTextPlain(cause.getPayload())));
              } else {
                asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
              }
            } else {
              log.debug("Preparing response to client");
              asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse
                .respond201WithApplicationJson(reply.result().getEntity(), PostInvoiceStorageInvoicesResponse.headersFor201()
                  .withLocation(INVOICE_PREFIX + reply.result().getEntity().getId()))));
            }
          });
      });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INVOICE_TABLE, Invoice.class, id, okapiHeaders,vertxContext, GetInvoiceStorageInvoicesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(INVOICE_TABLE, id, okapiHeaders, vertxContext, DeleteInvoiceStorageInvoicesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putInvoiceStorageInvoicesById(String id, String lang, Invoice entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(INVOICE_TABLE, entity, id, okapiHeaders, vertxContext, PutInvoiceStorageInvoicesByIdResponse.class, reply -> {
      asyncResultHandler.handle(reply);
      deleteSequence(entity);
    });
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLines(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      EntitiesMetadataHolder<InvoiceLine, InvoiceLineCollection> entitiesMetadataHolder = new EntitiesMetadataHolder<>(InvoiceLine.class, InvoiceLineCollection.class, GetInvoiceStorageInvoiceLinesResponse.class);
      QueryHolder cql = new QueryHolder(INVOICE_LINE_TABLE, query, offset, limit, lang);
      getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
    });
  }

  @Validate
  @Override
  public void postInvoiceStorageInvoiceLines(String lang, InvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(INVOICE_LINE_TABLE, entity, okapiHeaders, vertxContext, PostInvoiceStorageInvoiceLinesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putInvoiceStorageInvoiceLinesById(String id, String lang, InvoiceLine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(INVOICE_LINE_TABLE, entity, id, okapiHeaders, vertxContext, PutInvoiceStorageInvoiceLinesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLinesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INVOICE_LINE_TABLE, InvoiceLine.class, id, okapiHeaders, vertxContext, GetInvoiceStorageInvoiceLinesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoiceLinesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(INVOICE_LINE_TABLE, id, okapiHeaders, vertxContext, DeleteInvoiceStorageInvoiceLinesByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getInvoiceStorageInvoiceNumber(String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(Response.status(501).build()));
  }
  
  /**
   * Creates a new sequence within a scope of transaction 
   *
   * @param Tx<Invoice>
   *          Transaction of type Invoice
   * @return Future of Invoice transaction with Invoice Line number sequence
   */
  private Future<Tx<Invoice>> createSequence(Tx<Invoice> tx) {
    Future<Tx<Invoice>> future = Future.future();

    String invoiceId = tx.getEntity().getId();
    log.debug("Creating IL number sequence for invoice with id={}", invoiceId);
    try {
      pgClient.execute(tx.getConnection(), CREATE_SEQUENCE.getQuery(invoiceId), reply -> {
        if (reply.failed()) {
          log.error("IL number sequence creation for invoice with id={} failed", reply.cause(), invoiceId);
          pgClient.rollbackTx(tx.getConnection(), rb -> future.fail(new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), reply.cause().getMessage())));
        } else {
          log.debug("IL number sequence for invoice with id={} successfully created", invoiceId);
          future.complete(tx);
        }
      });
    } catch (Exception e) {
      future.fail(new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()));
    }
    return future;
  }

  /**
   * Deletes a sequence associated with the Invoice
   *
   * @param Invoice
   *          Invoice with the sequence number to be deleted 
   */
  private void deleteSequence(Invoice invoice) {
    if(invoice.getStatus() == Invoice.Status.CANCELLED || invoice.getStatus() == Invoice.Status.PAID) {
      // Try to drop sequence for the IL number but ignore failures
      pgClient.execute(DROP_SEQUENCE.getQuery(invoice.getId()), reply -> {
        if (reply.failed()) {
          log.error("IL number sequence for invoice with id={} failed to be dropped", reply.cause(), invoice.getId());
        }
      });
    }
  }
  
  /**
   * Creates a new Invoice within the scope of its transaction 
   *
   * @param Tx<Invoice>
   *          Transaction of type Invoice
   * @return  Future of Invoice transaction with new Invoice
   */
  private Future<Tx<Invoice>> createInvoice(Tx<Invoice> tx) {
    Future<Tx<Invoice>> future = Future.future();

    Invoice invoice = tx.getEntity();
    if (invoice.getId() == null) {
    	invoice.setId(UUID.randomUUID().toString());
    }

    log.debug("Creating new invoice with id={}", invoice.getId());

    pgClient.save(tx.getConnection(), INVOICE_TABLE, invoice.getId(), invoice, reply -> {
      if(reply.failed()) {
        log.error("Invoice creation with id={} failed", reply.cause(), invoice.getId());
        pgClient.rollbackTx(tx.getConnection(), rb -> {
          String badRequestMessage = PgExceptionUtil.badRequestMessage(reply.cause());
          if (badRequestMessage != null) {
            future.fail(new HttpStatusException(Response.Status.BAD_REQUEST.getStatusCode(), badRequestMessage));
          } else {
            future.fail(new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), reply.cause().getMessage()));
          }
        });
      } else {
        log.debug("New invoice with id={} successfully created", invoice.getId());
        future.complete(tx);
      }
    });
    return future;
  }
  
  /**
   * Starts a new transaction to create a new Invoice and to generate a new sequence number 
   *
   * @param Tx<Invoice>
   *          Transaction of type Invoice
   * @return  Future of Invoice transaction with new Invoice
   */
  private Future<Tx<Invoice>> startTx(Tx<Invoice> tx) {
    Future<Tx<Invoice>> future = Future.future();

    log.debug("Start transaction");

    pgClient.startTx(sqlConnection -> {
      tx.setConnection(sqlConnection);
      future.complete(tx);
    });
    return future;
  }

  /**
   * Ends a transaction after creating a new Invoice and generating a new sequence number 
   *
   * @param Tx<Invoice>
   *          Transaction of type Invoice
   * @return  Future of Invoice transaction with new Invoice
   */
  private Future<Tx<Invoice>> endTx(Tx<Invoice> tx) {
    log.debug("End transaction");
    Future<Tx<Invoice>> future = Future.future();
    pgClient.endTx(tx.getConnection(), v -> future.complete(tx));
    return future;
  }
  
  public class Tx<T> {

    private T entity;
    private AsyncResult<SQLConnection> sqlConnection;

    Tx(T entity) {
      this.entity = entity;
    }

    public T getEntity() {
      return entity;
    }

    AsyncResult<SQLConnection> getConnection() {
      return sqlConnection;
    }

    void setConnection(AsyncResult<SQLConnection> sqlConnection) {
      this.sqlConnection = sqlConnection;
    }
  }
}
