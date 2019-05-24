package org.folio.rest.utils;


import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherLine;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoice.sample", "note", "Updated note for invoice", 0),
  INVOICE_LINES("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice_line.sample", "quantity", 5, 0),
  VOUCHER("/voucher-storage/vouchers", Voucher.class, "voucher.sample", "batchNumber", "202", 0),
  VOUCHER_LINES("/voucher-storage/voucher-lines", VoucherLine.class, "voucher_line.sample", "externalAccountNumber", "Comment from unit test", 0);

  // Examples should be replaced by sample data after MODINVOSTO-10 and MODINVOSTO-16
  private static final String EXAMPLES_PATH = "ramls/acq-models/mod-invoice-storage/examples/";

  TestEntities(String endpoint, Class<?> clazz, String sampleFileName, String updatedFieldName, Object updatedFieldValue, int initialQuantity) {
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.sampleFileName = sampleFileName;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.initialQuantity = initialQuantity;
  }

  private int initialQuantity;
  private String endpoint;
  private String sampleFileName;
  private String updatedFieldName;
  private Object updatedFieldValue;
  private Class<?> clazz;

  public String getEndpoint() {
    return endpoint;
  }

  public String getEndpointWithId() {
    return endpoint + "/{id}";
  }

  public String getSampleFileName() {
    return EXAMPLES_PATH + sampleFileName;
  }

  public String getUpdatedFieldName() {
    return updatedFieldName;
  }

  public Object getUpdatedFieldValue() {
    return updatedFieldValue;
  }

  public int getInitialQuantity() {
    return initialQuantity;
  }

  public Class<?> getClazz() {
    return clazz;
  }
}
