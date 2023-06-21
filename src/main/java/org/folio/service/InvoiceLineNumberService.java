package org.folio.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.invoice.InvoiceDAO;
import org.folio.dao.lines.InvoiceLinesDAO;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineNumber;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.CriterionBuilder;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.folio.rest.utils.ResponseUtils.buildErrorResponse;
import static org.folio.rest.utils.ResponseUtils.buildOkResponse;


public class InvoiceLineNumberService {
  private static final Logger log = LogManager.getLogger();

  private final InvoiceDAO invoiceDAO;
  private final InvoiceLinesDAO invoiceLinesDAO;


  public InvoiceLineNumberService(InvoiceDAO invoiceDAO, InvoiceLinesDAO invoiceLinesDAO) {
    this.invoiceDAO = invoiceDAO;
    this.invoiceLinesDAO = invoiceLinesDAO;
  }

  public void getInvoiceStorageInvoiceLineNumber(String invoiceId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.debug("Trying to get invoice line number for invoice {}", invoiceId);
    if (invoiceId == null) {
      asyncResultHandler.handle(buildErrorResponse(new HttpException(Response.Status.BAD_REQUEST.getStatusCode(),
        "invoiceId is required")));
      return;
    }
    vertxContext.runOnContext((Void v) -> {
      DBClient dbClient = new DBClient(vertxContext, okapiHeaders);
      retrieveNewLineNumber(invoiceId, dbClient)
        .onSuccess(invoiceLineNumber -> asyncResultHandler.handle(buildOkResponse(invoiceLineNumber)))
        .onFailure(t -> asyncResultHandler.handle(buildErrorResponse(t)));
    });
  }

  public Future<InvoiceLineNumber> retrieveNewLineNumber(String invoiceId, DBClient dbClient) {
    PostgresClient pgClient = dbClient.getPgClient();
    log.debug("retrieveNewLineNumber: getting invoice {} for update", invoiceId);
    return pgClient.withTrans(conn -> invoiceDAO.getInvoiceByIdForUpdate(invoiceId, conn)
      .compose(invoice -> {
        if (invoice.getNextInvoiceLineNumber() != null)
          return Future.succeededFuture(invoice);
        log.warn("nextInvoiceLineNumber missing for invoice {}, calculating from lines", invoiceId);
        return getLastLineNumber(invoiceId, conn)
          .map(lastUsedNumber -> invoice.withNextInvoiceLineNumber(lastUsedNumber + 1));
      })
      .compose(invoice -> {
        log.debug("Updating invoice {} with new nextInvoiceLineNumber", invoiceId);
        int nextNumber = invoice.getNextInvoiceLineNumber();
        invoice.setNextInvoiceLineNumber(nextNumber + 1);
        return invoiceDAO.updateInvoice(invoice, conn)
          .map(v -> nextNumber);
      })
      .map(n -> {
        log.debug("retrieveNewLineNumber: done, invoice {}", invoiceId);
        return new InvoiceLineNumber().withSequenceNumber(n.toString());
      })
      .onSuccess(number -> log.info("Successfully retrieved invoice line number {} for invoice {}",
        number.getSequenceNumber(), invoiceId))
      .onFailure(t -> log.error("Error while attempting to retrieve invoice line number for invoice {}",
        invoiceId, t))
    );
  }

  public Future<Integer> getLastLineNumber(String invoiceId, Conn conn) {
    return getInvoiceLinesByInvoiceId(invoiceId, conn)
      .map(lines -> this.getLastLineNumber(invoiceId, lines));
  }

  private int getLastLineNumber(String invoiceId, List<InvoiceLine> invoiceLines) {
    try {
      return invoiceLines.stream()
        .map(InvoiceLine::getInvoiceLineNumber)
        .filter(Objects::nonNull)
        .map(Integer::parseInt)
        .reduce(Integer::max)
        .orElse(0);
    } catch (Exception t) {
      log.warn("Error calculating a new line number based on existing lines, invoiceId={}", invoiceId, t);
      return 0;
    }
  }

  private Future<List<InvoiceLine>> getInvoiceLinesByInvoiceId(String invoiceId, Conn conn) {
    Criterion criterion = new CriterionBuilder()
      .with("invoiceId", invoiceId)
      .build();
    return invoiceLinesDAO.getInvoiceLines(criterion, conn)
      .onFailure(t -> log.error("Retrieve invoice lines failed, invoiceId={}", invoiceId, t))
      .onSuccess(result -> log.trace("Retrieved invoice lines, invoiceId={}", invoiceId));
  }
}
