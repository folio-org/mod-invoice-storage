package org.folio.dao.lines;

import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.Criteria.Criterion;

import java.util.List;

import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_LINE_TABLE;

public class InvoiceLinesPostgresDAO implements InvoiceLinesDAO {
  private final Logger log = LogManager.getLogger();

  @Override
  public Future<List<InvoiceLine>> getInvoiceLines(Criterion criterion, Conn conn) {
    log.trace("InvoiceLinesPostgresDAO getInvoiceLines, criterion={}", criterion);
    return conn.get(INVOICE_LINE_TABLE, InvoiceLine.class, criterion, false)
      .map(results -> {
        log.trace("getInvoiceLines success, criterion={}", criterion);
        return results.getResults();
      })
      .onFailure(t -> log.error("getInvoiceLines failed, criterion={}", criterion, t));
  }
}
