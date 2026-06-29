package org.folio.dao.lines;

import io.vertx.core.Future;
import io.vertx.ext.web.handler.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.DbUtils;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;

import java.util.List;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_LINE_TABLE;
import static org.folio.rest.utils.ResponseUtils.convertPgExceptionIfNeeded;

public class InvoiceLinesPostgresDAO implements InvoiceLinesDAO {
  private final Logger log = LogManager.getLogger();

  @Override
  public Future<List<InvoiceLine>> getInvoiceLines(Criterion criterion, Conn conn) {
    log.trace("getInvoiceLines:: Getting invoice lines with criterion: {}", criterion);
    return conn.get(INVOICE_LINE_TABLE, InvoiceLine.class, criterion, false)
      .map(Results::getResults)
      .onSuccess(lines -> log.trace("getInvoiceLines:: Got {} invoice lines with criterion: {}", lines.size(), criterion))
      .onFailure(t -> log.error("Failed to get invoice lines with criterion: {}", criterion, t));
  }

  @Override
  public Future<InvoiceLine> getInvoiceLineByIdForUpdate(String id, Conn conn) {
    return conn.getByIdForUpdate(INVOICE_LINE_TABLE, id, InvoiceLine.class)
      .map(invoiceLine -> {
        if (invoiceLine == null) {
          throw new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase());
        }
        return invoiceLine;
      })
      .onFailure(t -> log.error("getInvoiceLineByIdForUpdate failed for invoice line with id {}", id, t));
  }

  @Override
  public Future<String> createInvoiceLine(InvoiceLine invoiceLine, Conn conn) {
    log.trace("createInvoiceLine:: Creating invoice line: {}", invoiceLine);
    return conn.save(INVOICE_LINE_TABLE, invoiceLine.getId(), invoiceLine, true)
      .recover(t -> Future.failedFuture(convertPgExceptionIfNeeded(t)))
      .onSuccess(invoiceLineId -> log.info("createInvoiceLine:: Created invoice line with id: {}", invoiceLineId))
      .onFailure(t -> log.error("Failed to create invoice line with id: {}", invoiceLine.getId(), t));
  }

  @Override
  public Future<Void> updateInvoiceLine(String id, InvoiceLine invoiceLine, Conn conn) {
    log.trace("updateInvoiceLine:: Updating invoice line: {}", invoiceLine);
    return conn.update(INVOICE_LINE_TABLE, invoiceLine, id)
      .compose(DbUtils::verifyEntityUpdate)
      .onSuccess(v -> log.info("updateInvoiceLine:: Updated invoice line with id: {}", id))
      .onFailure(t -> log.error("Failed to update invoice line with id: {}", id, t))
      .mapEmpty();
  }

}
