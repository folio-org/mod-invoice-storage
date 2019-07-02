package org.folio.rest.utils;


import org.folio.rest.jaxrs.model.AcquisitionsUnitAssignment;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherLine;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoices/123invoicenumber45_approved_for_2_orders.json", "note", "Updated note for invoice", 8),
  INVOICE_LINES("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice-lines/123invoicenumber45-1.json", "quantity", 5, 9),
  VOUCHER("/voucher-storage/vouchers", Voucher.class, "vouchers/test_voucher.json", "batchNumber", "202", 1),
  VOUCHER_LINES("/voucher-storage/voucher-lines", VoucherLine.class, "voucher-lines/test_voucher_line.json", "externalAccountNumber", "Comment from unit test", 1),
  ACQUISITIONS_UNIT_ASSIGNMENTS("/invoice-storage/acquisitions-unit-assignments", AcquisitionsUnitAssignment.class, "acquisitions-unit-assignments/AUA-5c499782.json", "recordId", "733cafd3-895f-4e33-87b7-bf40dc3c8069", 1);

  private static final String SAMPLES_PATH = "data/";

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
    return SAMPLES_PATH + sampleFileName;
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
