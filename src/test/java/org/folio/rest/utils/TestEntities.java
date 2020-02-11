package org.folio.rest.utils;

import org.folio.rest.jaxrs.model.BatchGroup;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherLine;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoices/123invoicenumber45_approved_for_2_orders.json", "note", "Updated note for invoice", 8,0),
  INVOICE_LINES("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice-lines/123invoicenumber45-1.json", "quantity", 5, 9,0),
  VOUCHER("/voucher-storage/vouchers", Voucher.class, "vouchers/test_voucher.json", "exportToAccounting", true, 1,0),
  VOUCHER_LINES("/voucher-storage/voucher-lines", VoucherLine.class, "voucher-lines/test_voucher_line.json", "externalAccountNumber", "Comment from unit test", 1,0),
  BATCH_GROUP("/batch-group-storage/batch-groups", BatchGroup.class, "batch-groups/test-batch-group.json", "name", "folio",2,1);

  private static final String SAMPLES_PATH = "data/";
  TestEntities(String endpoint, Class<?> clazz, String sampleFileName, String updatedFieldName, Object updatedFieldValue, int initialQuantity, int systemDataQuantity) {
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.sampleFileName = sampleFileName;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.initialQuantity = initialQuantity;
    this.systemDataQuantity = systemDataQuantity;
  }

  private int initialQuantity;
  private int systemDataQuantity;
  private String endpoint;
  private String sampleId;
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

  public String getId() {
    return sampleId;
  }

  public void setId(String id) {
    this.sampleId = id;
  }

  public int getSystemDataQuantity() {
    return systemDataQuantity;
  }
}
