package org.folio.dao.invoice;

import io.vertx.core.Future;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.persist.DBClient;

public interface InvoiceDAO {

  Future<DBClient> createSequence(String invoiceId, DBClient client);
  void deleteSequence(Invoice invoice, DBClient client);
  Future<DBClient> createInvoice(Invoice invoice, DBClient client);
  Future<DBClient> deleteInvoice(String id, DBClient client);
  Future<DBClient> deleteSequenceByInvoiceId(String id, DBClient client);
  Future<DBClient> deleteInvoiceLinesByInvoiceId(String id, DBClient client);
  Future<DBClient> createInvoiceDocument(InvoiceDocument invoiceDoc, DBClient client);
  Future<InvoiceDocument> getInvoiceDocument(String invoiceId, String documentId, DBClient client);

}
