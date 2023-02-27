package org.folio.dao.invoice;

import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_ID_FIELD_NAME;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_LINE_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import static org.folio.rest.utils.HelperUtils.SequenceQuery.CREATE_SEQUENCE;
import static org.folio.rest.utils.HelperUtils.SequenceQuery.DROP_SEQUENCE;
import static org.folio.rest.utils.ResponseUtils.handleFailure;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Contents;
import org.folio.rest.jaxrs.model.DocumentMetadata;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.sqlclient.Tuple;

public class InvoicePostgresDAO implements InvoiceDAO {

  private final Logger log = LogManager.getLogger(this.getClass());

  /**
   * Creates a new sequence within a scope of transaction
   *
   * @return Future of Invoice transaction with Invoice Line number sequence
   */
  public Future<DBClient> createSequence(String invoiceId, DBClient client) {
    log.info("Creating IL number sequence for invoice with id={}", invoiceId);
    Promise<DBClient> promise = Promise.promise();
    try {
      client.getPgClient().execute(client.getConnection(), CREATE_SEQUENCE.getQuery(invoiceId), reply -> {
        if (reply.failed()) {
          String errorMessage = String.format("Invoice Line number sequence creation for invoice with id=%s failed", invoiceId);
          log.error(errorMessage, reply.cause());
          handleFailure(promise, reply);
        } else {
          log.info("IL number sequence for invoice with id={} successfully created", invoiceId);
          promise.complete(client);
        }
      });
    } catch (Exception e) {
      log.error("Error when creating sequence for invoice with id={}", invoiceId, e);
      promise.fail(new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()));
    }
    return promise.future();
  }

  /**
   * Deletes a sequence associated with the invoice if it has been cancelled or paid.
   * Does not return an error if it fails.
   *
   * @param invoice Invoice with the sequence number to be deleted
   */
  public void deleteSequence(Invoice invoice, DBClient client) {
    final String id = invoice.getId();
    if (invoice.getStatus() == Invoice.Status.CANCELLED || invoice.getStatus() == Invoice.Status.PAID) {
      log.info("Delete sequence with invoice id={}", invoice.getId());
      String sqlQuery = DROP_SEQUENCE.getQuery(id);
      // Try to drop sequence for the IL number but ignore failures
      log.info("deleteSequence drop sequence query -- {}", sqlQuery);
      client.getPgClient().execute(sqlQuery, reply -> {
        if (reply.failed()) {
          String errorMessage = String.format("Invoice Line number sequence for invoice with id=%s failed to be dropped", id);
          log.error(errorMessage, reply.cause());
        }
      });
    }
  }

  public Future<DBClient> createInvoice(Invoice invoice, DBClient client) {
    log.info("Creating new invoice with id={}", invoice.getId());
    Promise<DBClient> promise = Promise.promise();
    if (invoice.getId() == null) {
      invoice.setId(UUID.randomUUID().toString());
    }
    client.getPgClient().save(client.getConnection(), INVOICE_TABLE, invoice.getId(), invoice, reply -> {
      if (reply.failed()) {
        String errorMessage = String.format("Invoice creation with id=%s failed", invoice.getId());
        log.error(errorMessage, reply.cause());
        handleFailure(promise, reply);
      } else {
        log.info("New invoice with id={} successfully created", invoice.getId());
        promise.complete(client);
      }
    });
    return promise.future();
  }

  public Future<DBClient> deleteInvoice(String id, DBClient client) {
    log.info("Delete invoice with id={}", id);
    Promise<DBClient> promise = Promise.promise();
    client.getPgClient().delete(client.getConnection(), INVOICE_TABLE, id, reply -> {
      if (reply.failed()) {
        log.error("Invoice deletion with id={} failed", id, reply.cause());
        handleFailure(promise, reply);
      } else {
        if (reply.result().rowCount() == 0) {
          promise.fail(new HttpException(Response.Status.NOT_FOUND.getStatusCode(), "Invoice not found"));
        } else {
          log.info("Invoice with id={} successfully deleted", id);
          promise.complete(client);
        }
      }
    });
    return promise.future();
  }

  public Future<DBClient> deleteSequenceByInvoiceId(String id, DBClient client) {
    String sqlQuery = DROP_SEQUENCE.getQuery(id);
    log.info("InvoiceStorageImpl deleteSequence Drop sequence query -- {}", sqlQuery);
    Promise<DBClient> promise = Promise.promise();
    client.getPgClient().execute(client.getConnection(), sqlQuery, reply -> {
      if (reply.failed()) {
        String errorMessage = String.format("Invoice Line number sequence for invoice with id=%s failed to be dropped", id);
        log.error(errorMessage, reply.cause(), id);
        handleFailure(promise, reply);
      } else {
        log.info("Invoice Line number sequence for invoice with id={} successfully dropped", id);
        promise.complete(client);
      }
    });
    return promise.future();
  }

  public Future<DBClient> deleteInvoiceLinesByInvoiceId(String id, DBClient client) {
    log.info("Delete invoice lines by invoice id={}", id);
    Promise<DBClient> promise = Promise.promise();
    Criterion criterion = getCriterionByFieldNameAndValue(INVOICE_ID_FIELD_NAME, id);
    client.getPgClient().delete(client.getConnection(), INVOICE_LINE_TABLE, criterion, reply -> {
      if (reply.failed()) {
        String errorMessage = String.format("The invoice %s cannot be deleted", id);
        log.error(errorMessage, reply.cause());
        handleFailure(promise, reply);
      } else {
        log.info("{} invoice lines of invoice with id={} successfully deleted", reply.result().rowCount(), id);
        promise.complete(client);
      }
    });
    return promise.future();
  }

  public Future<DBClient> deleteInvoiceDocumentsByInvoiceId(String id, DBClient client) {
    log.info("Delete invoice documents by invoice id={}", id);
    Promise<DBClient> promise = Promise.promise();
    Criterion criterion = getCriterionByFieldNameAndValue(INVOICE_ID_FIELD_NAME, id);
    client.getPgClient().delete(client.getConnection(), DOCUMENT_TABLE, criterion, reply -> {
      if (reply.failed()) {
        String errorMessage = String.format("The documents linked to invoice %s could not be deleted", id);
        log.error(errorMessage, reply.cause());
        handleFailure(promise, reply);
      } else {
        log.info("{} documents of invoice with id={} were successfully deleted", reply.result().rowCount(), id);
        promise.complete(client);
      }
    });
    return promise.future();

  }

  public Future<DBClient> createInvoiceDocument(InvoiceDocument invoiceDoc, DBClient client) {
    log.info("create invoice document with id={}", invoiceDoc.getDocumentMetadata().getId());
    Promise<DBClient> promise = Promise.promise();
    try {
      String fullTableName = PostgresClient.convertToPsqlStandard(client.getTenantId()) + "." + DOCUMENT_TABLE;
      invoiceDoc.getDocumentMetadata().setMetadata(invoiceDoc.getMetadata());
      boolean base64Exists = invoiceDoc.getContents() != null && StringUtils.isNotEmpty(invoiceDoc.getContents().getData());
      String sql = "INSERT INTO " + fullTableName + " (id, " + (base64Exists ? "document_data," : "") +
        " jsonb) VALUES ($1," + (base64Exists ? "$2,$3" : "$2") + ") RETURNING id";
      client.getPgClient().execute(sql, prepareDocumentQueryParams(invoiceDoc), reply -> {
        if (reply.succeeded()) {
          promise.complete(client);
        } else {
          handleFailure(promise, reply);
        }
      });
    } catch (Exception e) {
      log.error("Error while creating invoice document with id: {}", invoiceDoc.getDocumentMetadata().getInvoiceId(), e);
      promise.fail(new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()));
    }
    return promise.future();
  }

  public Future<InvoiceDocument> getInvoiceDocument(String invoiceId, String documentId, DBClient client) {
    log.info("get invoice document with invoiceId={} and documentId={}", invoiceId, documentId);
    Promise<InvoiceDocument> promise = Promise.promise();
    try {
      String fullTableName = PostgresClient.convertToPsqlStandard(client.getTenantId()) + "." + DOCUMENT_TABLE;
      String query = "SELECT jsonb, document_data FROM " + fullTableName + " WHERE id ='" + documentId +
        "' AND invoiceId='" + invoiceId + "'";
      client.getPgClient().select(query, reply -> {
        try {
          if (reply.succeeded()) {
            if (reply.result().rowCount() == 0) {
              log.error("Invoice document with invoiceId={} and documentId={} not found", invoiceId, documentId);
              promise.fail(new HttpException(Response.Status.NOT_FOUND.getStatusCode(),
                Response.Status.NOT_FOUND.getReasonPhrase()));
              return;
            }
            InvoiceDocument invoiceDocument = new InvoiceDocument();

            JsonObject resultJson = JsonObject.mapFrom(reply.result().iterator().next().getValue(0));

            DocumentMetadata documentMetadata = (resultJson).mapTo(DocumentMetadata.class);
            invoiceDocument.setDocumentMetadata(documentMetadata);
            invoiceDocument.setMetadata(documentMetadata.getMetadata());

            String base64Content = reply.result().iterator().next().getString(1);
            if (StringUtils.isNotEmpty(base64Content)){
              invoiceDocument.setContents(new Contents().withData(base64Content));
            }
            log.info("Invoice document with invoiceId={} and documentId={} successfully retrieved", invoiceId, documentId);
            promise.complete(invoiceDocument);
          } else {
            log.error("Error while getting invoice document with invoiceId={} and documentId={}", invoiceId, documentId, reply.cause());
            handleFailure(promise, reply);
          }
        } catch (Exception e) {
          log.error("SQL Error while getting invoice document with invoiceId={} and documentId={}", invoiceId, documentId, e);
          promise.fail(new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase()));
        }
      });
    } catch (Exception e) {
      log.error("Error while getting invoice document with invoiceId={} and documentId={}", invoiceId, documentId, e);
      promise.fail(new HttpException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
        Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase()));
    }
    return promise.future();
  }

  private Criterion getCriterionByFieldNameAndValue(String fieldName, String fieldValue) {
    Criteria a = new Criteria();
    a.addField("'" + fieldName + "'");
    a.setOperation("=");
    a.setVal(fieldValue);
    return new Criterion(a);
  }

  private Tuple prepareDocumentQueryParams(InvoiceDocument entity) {

    if (entity.getDocumentMetadata().getId() == null) {
      entity.getDocumentMetadata().setId(UUID.randomUUID().toString());
    }

    if (entity.getContents() != null && StringUtils.isNotEmpty(entity.getContents().getData())) {
      return Tuple.of(UUID.fromString(entity.getDocumentMetadata().getId()), entity.getContents().getData(),
        JsonObject.mapFrom(entity.getDocumentMetadata()));
    }

    return Tuple.of(UUID.fromString(entity.getDocumentMetadata().getId()),
      JsonObject.mapFrom(entity.getDocumentMetadata()));
  }

}
