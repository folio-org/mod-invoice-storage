package org.folio.rest.utils;

import org.folio.rest.jaxrs.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoices/123invoicenumber45_approved_for_2_orders.json", "note", "Updated note for invoice", 8, 0, true),
  INVOICE_LINES("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice-lines/123invoicenumber45-1.json", "quantity", 5, 9, 0, true),
  VOUCHER("/voucher-storage/vouchers", Voucher.class, "vouchers/test_voucher.json", "exportToAccounting", true, 1, 0, true),
  VOUCHER_LINES("/voucher-storage/voucher-lines", VoucherLine.class, "voucher-lines/test_voucher_line.json", "externalAccountNumber", "Comment from unit test", 1, 0, true),
  BATCH_GROUP("/batch-group-storage/batch-groups", BatchGroup.class, "batch-groups/test-batch-group.json", "name", "folio", 2, 1, true),
  BATCH_VOUCHER_EXPORT_CONFIGS("/batch-voucher-storage/export-configurations", ExportConfig.class, "batch-voucher-export-configs/test_config.json", "enableScheduledExport", false, 0, 0, true),
  BATCH_VOUCHER("/batch-voucher-storage/batch-vouchers", BatchVoucher.class, "batch-voucher-exports/batch-vouchers/test-batch-voucher.json", "batchGroup", null, 0, 0, false),
  BATCH_VOUCHER_EXPORTS("/batch-voucher-storage/batch-voucher-exports", BatchVoucherExport.class, "batch-voucher-exports/test-batch-voucher-export.json", "message", "test", 0, 0, true);

  private static final String SAMPLES_PATH = "data/";

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
