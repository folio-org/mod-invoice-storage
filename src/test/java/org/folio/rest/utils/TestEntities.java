package org.folio.rest.utils;


import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherLine;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoices/test_invoice.json", "note", "Updated note for invoice", 1),
  INVOICE_LINES("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice-lines/test_invoice_line.json", "quantity", 5, 0),
  VOUCHER("/voucher-storage/vouchers", Voucher.class, "vouchers/test_voucher.json", "batchNumber", "202", 1),
  VOUCHER_LINES("/voucher-storage/voucher-lines", VoucherLine.class, "voucher-lines/test_voucher_line.json", "externalAccountNumber", "Comment from unit test", 0);

  // Examples should be replaced by sample data after MODINVOSTO-10 and MODINVOSTO-16
  private static final String EXAMPLES_PATH = "data/";

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
