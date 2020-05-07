package org.folio.rest.utils;

import org.folio.rest.jaxrs.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, TestData.Invoice.DEFAULT, "note", "Updated note for invoice", 0, 0, true),
  INVOICE_LINES("/invoice-storage/invoice-lines", InvoiceLine.class, TestData.InvoiceLines.DEFAULT, "quantity", 5, 0, 0, true),
  VOUCHER("/voucher-storage/vouchers", Voucher.class, TestData.Voucher.DEFAULT, "exportToAccounting", true, 0, 0, true),
  VOUCHER_LINES("/voucher-storage/voucher-lines", VoucherLine.class, TestData.VoucherLines.DEFAULT, "externalAccountNumber", "Comment from unit test", 0, 0, true),
  BATCH_GROUP("/batch-group-storage/batch-groups", BatchGroup.class, TestData.BatchGroup.DEFAULT, "name", "folio", 2, 1, true),
  BATCH_VOUCHER_EXPORT_CONFIGS("/batch-voucher-storage/export-configurations", ExportConfig.class, TestData.BatchVoucherExportConfigs.DEFAULT, "enableScheduledExport", false, 0, 0, true),
  BATCH_VOUCHER("/batch-voucher-storage/batch-vouchers", BatchVoucher.class, TestData.BatchVoucher.DEFAULT, "batchGroup", null, 0, 0, false),
  BATCH_VOUCHER_EXPORTS("/batch-voucher-storage/batch-voucher-exports", BatchVoucherExport.class, TestData.BatchVoucherExports.DEFAULT, "message", "test", 0, 0, true);

  TestEntities(String endpoint, Class<?> clazz, String sampleFileName, String updatedFieldName, Object updatedFieldValue, int initialQuantity, int systemDataQuantity, boolean collection) {
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.sampleFileName = sampleFileName;
    this.updatedFieldName = updatedFieldName;
    this.updatedFieldValue = updatedFieldValue;
    this.initialQuantity = initialQuantity;
    this.systemDataQuantity = systemDataQuantity;
    this.collection = collection;
  }

  private int initialQuantity;
  private int systemDataQuantity;
  private String endpoint;
  private String sampleId;
  private String sampleFileName;
  private String updatedFieldName;
  private Object updatedFieldValue;
  private Class<?> clazz;
  private boolean collection;

  public String getEndpoint() {
    return endpoint;
  }

  public String getEndpointWithId() {
    return endpoint + "/{id}";
  }

  public String getSampleFileName() {
    return sampleFileName;
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

  public int getEstimatedSystemDataRecordsQuantity() {
    return systemDataQuantity;
  }

  public boolean isCollection() {
    return collection;
  }

  public static List<TestEntities> getCollectableEntities() {
    return Arrays.stream(TestEntities.values()).filter(TestEntities::isCollection).collect(Collectors.toList());
  }
}
