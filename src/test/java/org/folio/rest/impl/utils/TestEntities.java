package org.folio.rest.impl.utils;


import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;


public enum TestEntities {
  INVOICE("/invoice-storage/invoices", Invoice.class, "invoice.sample"),
  INVOICE_LINE("/invoice-storage/invoice-lines", InvoiceLine.class, "invoice_line.sample");

  TestEntities(String endpoint, Class<?> clazz, String sampleFileName) {
    this.endpoint = endpoint;
    this.clazz = clazz;
    this.sampleFileName = sampleFileName;
  }

  private String endpoint;
  private String sampleFileName;
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

  public Class<?> getClazz() {
    return clazz;
  }
}
