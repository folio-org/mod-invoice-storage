package org.folio.dao.invoice;

import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.DBClient;

import io.vertx.core.Future;

public interface InvoiceDAO {

  Future<Invoice> getInvoiceByIdForUpdate(String invoiceId, Conn conn);
  Future<String> createInvoice(Invoice invoice, Conn conn);
  Future<Void> updateInvoice(String id, Invoice invoice, Conn conn);
  Future<DBClient> deleteInvoice(String id, DBClient client);
  Future<DBClient> deleteInvoiceLinesByInvoiceId(String id, DBClient client);
  Future<DBClient> deleteInvoiceDocumentsByInvoiceId(String id, DBClient client);
  Future<DBClient> createInvoiceDocument(InvoiceDocument invoiceDoc, DBClient client);
  Future<InvoiceDocument> getInvoiceDocument(String invoiceId, String documentId, DBClient client);

}
