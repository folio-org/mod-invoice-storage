package org.folio.dao.invoice;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.impl.InvoiceStorageImpl.DOCUMENT_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_ID_FIELD_NAME;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_LINE_TABLE;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import static org.folio.rest.utils.ResponseUtils.convertPgExceptionIfNeeded;
import static org.folio.rest.utils.ResponseUtils.handleFailure;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.DbUtils;
import org.folio.dbschema.ObjectMapperTool;
import org.folio.rest.jaxrs.model.Contents;
import org.folio.rest.jaxrs.model.DocumentMetadata;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.persist.Conn;
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


  @Override
  public Future<Invoice> getInvoiceByIdForUpdate(String invoiceId, Conn conn) {
    return conn.getByIdForUpdate(INVOICE_TABLE, invoiceId, Invoice.class)
      .map(invoice -> {
        if (invoice == null) {
          throw new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase());
        }
        return invoice;
      })
      .onFailure(t -> log.error("getInvoiceByIdForUpdate failed for invoice with id {}", invoiceId, t));
  }

  @Override
  public Future<String> createInvoice(Invoice invoice, Conn conn) {
    log.info("Creating new invoice with id={}", invoice.getId());
    if (invoice.getId() == null) {
      invoice.setId(UUID.randomUUID().toString());
    }
    if (invoice.getNextInvoiceLineNumber() == null) {
      invoice.setNextInvoiceLineNumber(1);
    }
    return conn.save(INVOICE_TABLE, invoice.getId(), invoice, true)
      .recover(t -> Future.failedFuture(convertPgExceptionIfNeeded(t)))
      .onFailure(t -> log.error("createInvoice failed for invoice with id {}", invoice.getId(), t))
      .onSuccess(s -> log.info("createInvoice:: New invoice with id: '{}' successfully created", invoice.getId()));
  }

  @Override
  public Future<Void> updateInvoice(String id, Invoice invoice, Conn conn) {
    return conn.update(INVOICE_TABLE, invoice, id)
      .compose(DbUtils::verifyEntityUpdate)
      .onSuccess(v -> log.info("updateInvoice:: Invoice with id: '{}' successfully updated", invoice.getId()))
      .onFailure(t -> log.error("Update failed for invoice with id {}", invoice.getId(), t))
      .mapEmpty();
  }

  @Override
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

  @Override
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

  @Override
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

  @Override
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

  @Override
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
            if (StringUtils.isNotEmpty(base64Content)) {
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
        new JsonObject(ObjectMapperTool.valueAsString(entity.getDocumentMetadata())));
    }

    return Tuple.of(UUID.fromString(entity.getDocumentMetadata().getId()),
      new JsonObject(ObjectMapperTool.valueAsString(entity.getDocumentMetadata())));
  }

}
