package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceCollection;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorage;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.EntitiesMetadataHolder;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.QueryHolder;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.persist.HelperUtils.SequenceQuery.CREATE_SEQUENCE;
import static org.folio.rest.persist.HelperUtils.SequenceQuery.DROP_SEQUENCE;
import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;

public class InvoiceStorageImpl implements InvoiceStorage {

  private static final Logger log = LoggerFactory.getLogger(InvoiceStorageImpl.class);

  private static final String INVOICE_PREFIX = "/invoice-storage/invoices/";

  private PostgresClient pgClient;

  public static final String INVOICE_TABLE = "invoices";
  private static final String INVOICE_LINE_TABLE = "invoice_lines";
  private static final String ID_FIELD_NAME = "id";
  private static final String INVOICE_ID_FIELD_NAME = "invoiceId";

  public InvoiceStorageImpl(Vertx vertx, String tenantId) {
    pgClient = PostgresClient.getInstance(vertx, tenantId);
    pgClient.setIdField(ID_FIELD_NAME);
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
        log.info("Creating a new invoice");
        Future.succeededFuture(new Tx<>(entity))
          .compose(this::startTx)
          .compose(this::createInvoice)
          .compose(this::createSequence)
          .compose(this::endTx)
          .setHandler(reply -> {
            if (reply.failed()) {
              HttpStatusException cause = (HttpStatusException) reply.cause();
              if (cause.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond400WithTextPlain(cause.getPayload())));
              } else if (cause.getStatusCode() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond401WithTextPlain(cause.getPayload())));
              } else {
                asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
              }
            } else {
              log.info("Preparing response to client");
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
    PgUtil.getById(INVOICE_TABLE, Invoice.class, id, okapiHeaders, vertxContext, GetInvoiceStorageInvoicesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      vertxContext.runOnContext(v -> {
        TxWithId tx = new TxWithId(id);
        log.info("Delete invoice");
        startTxWithId(tx)
          .thenCompose(this::deleteInvoiceById)
          .thenCompose(this::deleteInvoiceLinesByInvoiceId)
          .thenCompose(empty -> endTxWithId(tx))
          .thenAccept(result -> {
            log.info("Preparing response to client");
            asyncResultHandler.handle(Future.succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond204()));
          })
          .exceptionally(t -> {
            endTxWithId(tx)
              .thenAccept(res -> {
                HttpStatusException cause = (HttpStatusException) t.getCause();
                if (cause.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                  asyncResultHandler.handle(Future.succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond400WithTextPlain(cause.getPayload())));
                } else if (cause.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                  asyncResultHandler.handle(Future.succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond404WithTextPlain(Response.Status.NOT_FOUND.getReasonPhrase())));
                } else {
                  asyncResultHandler.handle(Future.succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
                }
              });
            return null;
          });
      });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(PostInvoiceStorageInvoicesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
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

  /**
   * Creates a new sequence within a scope of transaction
   *
   * @param tx<Invoice> Transaction of type Invoice
   * @return Future of Invoice transaction with Invoice Line number sequence
   */
  private Future<Tx<Invoice>> createSequence(Tx<Invoice> tx) {
    Future<Tx<Invoice>> future = Future.future();

    String invoiceId = tx.getEntity().getId();
    log.info("Creating IL number sequence for invoice with id={}", invoiceId);
    try {
      pgClient.execute(tx.getConnection(), CREATE_SEQUENCE.getQuery(invoiceId), reply -> {
        if (reply.failed()) {
          log.error("IL number sequence creation for invoice with id={} failed", reply.cause(), invoiceId);
          pgClient.rollbackTx(tx.getConnection(), rb -> future.fail(new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), reply.cause().getMessage())));
        } else {
          log.info("IL number sequence for invoice with id={} successfully created", invoiceId);
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
   * @param invoice Invoice with the sequence number to be deleted
   */
  private void deleteSequence(Invoice invoice) {
    String sqlQuery = DROP_SEQUENCE.getQuery(invoice.getId());
    if (invoice.getStatus() == Invoice.Status.CANCELLED || invoice.getStatus() == Invoice.Status.PAID) {
      // Try to drop sequence for the IL number but ignore failures
      log.info("InvoiceStorageImpl deleteSequence Drop sequence query -- ", sqlQuery);
      pgClient.execute(sqlQuery, reply -> {
        if (reply.failed()) {
          log.error("IL number sequence for invoice with id={} failed to be dropped", reply.cause(), invoice.getId());
        }
      });
    }
  }

  /**
   * Creates a new Invoice within the scope of its transaction
   *
   * @param tx<Invoice> Transaction of type Invoice
   * @return Future of Invoice transaction with new Invoice
   */
  private Future<Tx<Invoice>> createInvoice(Tx<Invoice> tx) {
    Future<Tx<Invoice>> future = Future.future();

    Invoice invoice = tx.getEntity();
    if (invoice.getId() == null) {
      invoice.setId(UUID.randomUUID().toString());
    }

    log.info("Creating new invoice with id={}", invoice.getId());
    pgClient.save(tx.getConnection(), INVOICE_TABLE, invoice.getId(), invoice, reply -> {
      if (reply.failed()) {
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
        log.info("New invoice with id={} successfully created", invoice.getId());
        future.complete(tx);
      }
    });
    return future;
  }

  /**
   * Starts a new transaction to create a new Invoice and to generate a new sequence number
   *
   * @param tx<Invoice> Transaction of type Invoice
   * @return Future of Invoice transaction with new Invoice
   */
  private Future<Tx<Invoice>> startTx(Tx<Invoice> tx) {
    Future<Tx<Invoice>> future = Future.future();
    log.info("Start transaction");

    pgClient.startTx(sqlConnection -> {
      tx.setConnection(sqlConnection);
      future.complete(tx);
    });
    return future;
  }

  private CompletableFuture<TxWithId> startTxWithId(TxWithId tx) {
    CompletableFuture<TxWithId> future = new CompletableFuture<>();
    log.info("Start transaction");

    pgClient.startTx(sqlConnection -> {
      tx.setConnection(sqlConnection);
      future.complete(tx);
    });
    return future;
  }

  /**
   * Ends a transaction after creating a new Invoice and generating a new sequence number
   *
   * @param tx Transaction of type Invoice
   * @return Future of Invoice transaction with new Invoice
   */
  private Future<Tx<Invoice>> endTx(Tx<Invoice> tx) {
    log.info("End transaction");

    Future<Tx<Invoice>> future = Future.future();
    pgClient.endTx(tx.getConnection(), v -> future.complete(tx));
    return future;
  }

  private CompletableFuture<TxWithId> endTxWithId(TxWithId tx) {
    log.info("End transaction");

    CompletableFuture<TxWithId> future = new CompletableFuture<>();
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

  public class TxWithId {

    private AsyncResult<SQLConnection> sqlConnection;
    private String id;

    TxWithId(String id) {
      this.id = id;
    }

    AsyncResult<SQLConnection> getConnection() {
      return sqlConnection;
    }

    void setConnection(AsyncResult<SQLConnection> sqlConnection) {
      this.sqlConnection = sqlConnection;
    }

    public String getId() {
      return this.id;
    }
  }

  private CompletableFuture<TxWithId> deleteInvoiceById(TxWithId tx) {
    CompletableFuture<TxWithId> future = new CompletableFuture<>();
    Criterion criterion = getCriterionByFieldNameAndValue(ID_FIELD_NAME, tx.getId());

    log.info("Delete invoice with id={}", tx.getId());
    pgClient.delete(tx.getConnection(), INVOICE_TABLE, criterion, reply -> {
      if (reply.failed()) {
        log.error("Invoice deletion with id={} failed", reply.cause(), tx.getId());
        rollbackDeleteInvoiceTransaction(tx, future, reply);
      } else if (reply.result().getUpdated() == 0) {
        future.completeExceptionally(new HttpStatusException(Response.Status.NOT_FOUND.getStatusCode(), "Invoice not found"));
      } else {
        log.info("Invoice with id={} successfully deleted", tx.getId());
        future.complete(tx);
      }
    });
    return future;
  }

  private void rollbackDeleteInvoiceTransaction(TxWithId tx, CompletableFuture<TxWithId> future, AsyncResult<UpdateResult> reply) {
    pgClient.rollbackTx(tx.getConnection(), rb -> {
      String badRequestMessage = PgExceptionUtil.badRequestMessage(reply.cause());
      if (badRequestMessage != null) {
        future.completeExceptionally(new HttpStatusException(Response.Status.BAD_REQUEST.getStatusCode(), badRequestMessage));
      } else {
        future.completeExceptionally(new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), reply.cause().getMessage()));
      }
    });
  }

  private CompletableFuture<TxWithId> deleteInvoiceLinesByInvoiceId(TxWithId tx) {
    CompletableFuture<TxWithId> future = new CompletableFuture<>();
    Criterion criterion = getCriterionByFieldNameAndValue(INVOICE_ID_FIELD_NAME, tx.getId());

    log.info("Delete invoice lines by invoice id={}", tx.getId());
    pgClient.delete(tx.getConnection(), INVOICE_LINE_TABLE, criterion, reply -> {
      if (reply.failed()) {
        log.error("Delete invoice lines by invoice id={} failed", reply.cause(), tx.getId());
        rollbackDeleteInvoiceTransaction(tx, future, reply);
      } else {
        log.info("{} invoice lines of invoice with id={} successfully deleted", tx.getId(), reply.result().getKeys().size());
        future.complete(tx);
      }
    });
    return future;
  }

  private Criterion getCriterionByFieldNameAndValue(String filedName, String fieldValue) {
    Criteria a = new Criteria();
    a.addField("'" + filedName + "'");
    a.setOperation("=");
    a.setValue(fieldValue);
    return new Criterion(a);
  }
}
