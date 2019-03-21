package org.folio.rest.impl.utils;


import org.folio.rest.jaxrs.model.Invoice;
// import org.folio.rest.jaxrs.model.InvoiceLine;

public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoice.sample", "invoice", "invoice updated", 0);
  //INVOICE_LINE("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice_line.sample",  "invoice-lines", "invoice-lines updated", 0);

  TestEntities(String endpoint, Class<?> clazz, String sampleFileName, String updatedFieldName, String updatedFieldValue, int initialQuantity) {
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
  private String updatedFieldValue;
  private Class<?> clazz;

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

  public String getUpdatedFieldValue() {
    return updatedFieldValue;
  }

  public int getInitialQuantity() {
    return initialQuantity;
  }
  
  public Class<?> getClazz() {
    return clazz;
  }
}
