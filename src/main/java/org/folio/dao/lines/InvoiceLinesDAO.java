package org.folio.dao.lines;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.Criteria.Criterion;

import java.util.List;

public interface InvoiceLinesDAO {
  Future<List<InvoiceLine>> getInvoiceLines(Criterion criterion, Conn conn);
}
