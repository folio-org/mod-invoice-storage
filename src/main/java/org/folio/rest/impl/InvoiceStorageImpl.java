package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;
import static org.folio.rest.persist.HelperUtils.SequenceQuery.CREATE_SEQUENCE;
import static org.folio.rest.persist.HelperUtils.SequenceQuery.DROP_SEQUENCE;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.jaxrs.model.DocumentCollection;
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
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;

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

  private static final Logger log = LoggerFactory.getLogger(InvoiceStorageImpl.class);

  private static final String INVOICE_PREFIX = "/invoice-storage/invoices/";
  public static final String INVOICE_TABLE = "invoices";
  private static final String INVOICE_LINE_TABLE = "invoice_lines";
  private static final String DOCUMENT_TABLE = "document";
  private static final String INVOICE_ID_FIELD_NAME = "invoiceId";
  private PostgresClient pgClient;

  public InvoiceStorageImpl(Vertx vertx, String tenantId) {
    pgClient = PostgresClient.getInstance(vertx, tenantId);
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

        Tx<Invoice> tx = new Tx<>(entity);
        startTx(tx)
          .compose(this::createInvoice)
          .compose(this::createSequence)
          .compose(this::endTx)
          .setHandler(reply -> {
            if (reply.failed()) {
              // The result of rollback operation is not so important, main failure cause is used to build the response
              rollbackTransaction(tx).setHandler(res -> {
                HttpStatusException cause = (HttpStatusException) reply.cause();
                if (cause.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                  asyncResultHandler
                    .handle(succeededFuture(PostInvoiceStorageInvoicesResponse.respond400WithTextPlain(cause.getPayload())));
                } else if (cause.getStatusCode() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                  asyncResultHandler
                    .handle(succeededFuture(PostInvoiceStorageInvoicesResponse.respond401WithTextPlain(cause.getPayload())));
                } else {
                  asyncResultHandler.handle(succeededFuture(PostInvoiceStorageInvoicesResponse
                    .respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
                }
              });
            } else {
              log.info("Preparing response to client");
              asyncResultHandler.handle(succeededFuture(PostInvoiceStorageInvoicesResponse
                .respond201WithApplicationJson(reply.result().getEntity(), PostInvoiceStorageInvoicesResponse.headersFor201()
                  .withLocation(INVOICE_PREFIX + reply.result().getEntity().getId()))));
            }
          });
      });
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(
          PostInvoiceStorageInvoicesResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
    }
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INVOICE_TABLE, Invoice.class, id, okapiHeaders, vertxContext, GetInvoiceStorageInvoicesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      vertxContext.runOnContext(v -> {
        log.info("Delete invoice");

        Tx<String> tx = new Tx<>(id);
        startTx(tx)
          .compose(this::deleteInvoiceLinesByInvoiceId)
          .compose(this::deleteSequence)
          .compose(this::deleteInvoiceById)
          .compose(this::endTx)
          .setHandler(result -> {
            if (result.failed()) {
              HttpStatusException cause = (HttpStatusException) result.cause();
              log.error("Invoice {} and associated lines if any were failed to be deleted", cause, tx.getEntity());

              // The result of rollback operation is not so important, main failure cause is used to build the response
              rollbackTransaction(tx).setHandler(res -> {
                if (cause.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                  asyncResultHandler.handle(succeededFuture(
                    DeleteInvoiceStorageInvoicesByIdResponse.respond404WithTextPlain(Response.Status.NOT_FOUND.getReasonPhrase())));
                } else if (cause.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                  asyncResultHandler.handle(succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond400WithTextPlain(cause.getPayload())));
                } else {
                  asyncResultHandler.handle(succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond500WithTextPlain(
                    Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
                }
              });
            } else {
              log.info("Invoice {} and associated lines were successfully deleted", tx.getEntity());
              asyncResultHandler.handle(succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse.respond204()));
            }
          });
      });
    } catch (Exception e) {
      asyncResultHandler.handle(succeededFuture(DeleteInvoiceStorageInvoicesByIdResponse
        .respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
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
  public void getInvoiceStorageInvoicesDocumentsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
      log.info("Get document list by invoice id");
      vertxContext.runOnContext((Void v) -> {
        try {
          Criterion criterion = getCriterionByFieldNameAndValue(INVOICE_ID_FIELD_NAME, id);

        pgClient.get(DOCUMENT_TABLE, Document.class, criterion, false, reply -> {
            try {
              if (reply.succeeded()) {
                DocumentCollection collection = new DocumentCollection();
                List<Document> results = reply.result().getResults();
                collection.setDocuments(results);

                Integer totalRecords = reply.result().getResultInfo().getTotalRecords();
                collection.setTotalRecords(totalRecords);

                collection.getDocuments().forEach(document-> document.setContents(null));
                asyncResultHandler.handle(Future.succeededFuture(GetInvoiceStorageInvoicesDocumentsByIdResponse.respond200WithApplicationJson(collection)));
              } else {
                log.error(reply.cause().getMessage(), reply.cause());
                asyncResultHandler.handle(Future.succeededFuture(GetInvoiceStorageInvoicesDocumentsByIdResponse.respond400WithTextPlain(reply.cause().getMessage())));
              }
            } catch (Exception e) {
              log.error(e.getMessage(), e);
              asyncResultHandler.handle(Future.succeededFuture(GetInvoiceStorageInvoicesDocumentsByIdResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
            }
          });
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          asyncResultHandler.handle(Future.succeededFuture(GetInvoiceStorageInvoicesDocumentsByIdResponse.respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
        }
      });
    }

  @Validate
  @Override
  public void postInvoiceStorageInvoicesDocumentsById(String id, String lang, Document entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(DOCUMENT_TABLE, entity, okapiHeaders, vertxContext, PostInvoiceStorageInvoiceLinesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String id, String documentId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(DOCUMENT_TABLE, InvoiceLine.class, id, okapiHeaders, vertxContext, GetInvoiceStorageInvoiceLinesByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String id, String documentId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(DOCUMENT_TABLE, id, okapiHeaders, vertxContext, DeleteInvoiceStorageInvoiceLinesByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putInvoiceStorageInvoicesDocumentsByIdAndDocumentId(String id, String documentId, String lang, Document entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(succeededFuture(PutInvoiceStorageInvoicesDocumentsByIdAndDocumentIdResponse.respond501WithTextPlain(Response.Status.NOT_IMPLEMENTED.getReasonPhrase())));
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
          handleFailure(future, reply);
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
        handleFailure(future, reply);
      } else {
        log.info("New invoice with id={} successfully created", invoice.getId());
        future.complete(tx);
      }
    });
    return future;
  }

  private void handleFailure(Future future, AsyncResult reply) {
    String badRequestMessage = PgExceptionUtil.badRequestMessage(reply.cause());
    if (badRequestMessage != null) {
      future.fail(new HttpStatusException(Response.Status.BAD_REQUEST.getStatusCode(), badRequestMessage));
    } else {
      future.fail(new HttpStatusException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), reply.cause()
        .getMessage()));
    }
  }

  /**
   * Starts a new transaction
   *
   * @param tx transaction holder
   * @return Future with started transaction
   */
  private <T> Future<Tx<T>> startTx(Tx<T> tx) {
    Future<Tx<T>> future = Future.future();
    log.info("Start transaction");

    pgClient.startTx(sqlConnection -> {
      tx.setConnection(sqlConnection);
      future.complete(tx);
    });
    return future;
  }

  /**
   * Ends a transaction
   *
   * @param tx Transaction holder
   * @return future which is completed when transaction committed
   */
  private <T> Future<Tx<T>> endTx(Tx<T> tx) {
    log.info("End transaction");

    Future<Tx<T>> future = Future.future();
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

  private Future<Tx<String>> deleteInvoiceById(Tx<String> tx) {
    log.info("Delete invoice with id={}", tx.getEntity());

    Future<Tx<String>> future = Future.future();

    pgClient.delete(tx.getConnection(), INVOICE_TABLE, tx.getEntity(), reply -> {
      if (reply.failed()) {
        handleFailure(future, reply);
      } else {
        if (reply.result().getUpdated() == 0) {
          future.fail(new HttpStatusException(Response.Status.NOT_FOUND.getStatusCode(), "Invoice not found"));
        } else {
          future.complete(tx);
        }
      }
    });
    return future;
  }

  private Future<Void> rollbackTransaction(Tx<?> tx) {
    Future<Void> future = Future.future();
    if (tx.getConnection().failed()) {
      future.fail(tx.getConnection().cause());
    } else {
      pgClient.rollbackTx(tx.getConnection(), future);
    }
    return future;
  }

  private Future<Tx<String>> deleteSequence(Tx<String> txWithId) {
    Future<Tx<String>> future = Future.future();

    String sqlQuery = DROP_SEQUENCE.getQuery(txWithId.getEntity());
    log.info("InvoiceStorageImpl deleteSequence Drop sequence query -- ", sqlQuery);
    pgClient.execute(sqlQuery, reply -> {
      if (reply.failed()) {
        log.error("IL number sequence for invoice with id={} failed to be dropped", reply.cause(), txWithId.getEntity());
        handleFailure(future, reply);
      } else {
        future.complete(txWithId);
      }
    });
    return future;
  }

  private Future<Tx<String>> deleteInvoiceLinesByInvoiceId(Tx<String> tx) {
    Future<Tx<String>> future = Future.future();
    Criterion criterion = getCriterionByFieldNameAndValue(INVOICE_ID_FIELD_NAME, tx.getEntity());
    log.info("Delete invoice lines by invoice id={}", tx.getEntity());

    pgClient.delete(tx.getConnection(), INVOICE_LINE_TABLE, criterion, reply -> {
      if (reply.failed()) {
        log.error("The invoice {} cannot be deleted", reply.cause(), tx.getEntity());
        handleFailure(future, reply);
      } else {
        log.info("{} invoice lines of invoice with id={} successfully deleted", reply.result().getUpdated(), tx.getEntity());
        future.complete(tx);
      }
    });
    return future;
  }

  private Criterion getCriterionByFieldNameAndValue(String filedName, String fieldValue) {
    Criteria a = new Criteria();
    a.addField("'" + filedName + "'");
    a.setOperation("=");
    a.setVal(fieldValue);
    return new Criterion(a);
  }
}
