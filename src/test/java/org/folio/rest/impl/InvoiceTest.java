package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICE;

import java.net.MalformedURLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class InvoiceTest extends TestBase {
  private static final Logger LOGGER = LogManager.getLogger(InvoiceTest.class);

  @Test
  public void testDeleteInvoiceAndAssociatedLines() throws MalformedURLException {
    LOGGER.info(String.format("--- mod-invoice-storage %s test: Delete invoice and associated invoice lines", INVOICE.name()));
    String invoiceSample = getFile(INVOICE.getSampleFileName());
    JsonObject invoiceJson = new JsonObject(invoiceSample);
    invoiceJson.remove("id");
    invoiceSample = invoiceJson.toString();
    String invoiceSampleId = createEntity(INVOICE.getEndpoint(), invoiceSample);

    String invoiceLineSample = getFile(TestEntities.INVOICE_LINES.getSampleFileName());
    JsonObject invoiceLineJson = new JsonObject(invoiceLineSample);
    invoiceLineJson.remove("id");
    invoiceLineJson.put("invoiceId", invoiceSampleId);
    invoiceLineSample = invoiceLineJson.toString();

    String firstLineId = createEntity(TestEntities.INVOICE_LINES.getEndpoint(), invoiceLineSample);
    LOGGER.info("Created line with id={}", firstLineId);
    String secondLineId = createEntity(TestEntities.INVOICE_LINES.getEndpoint(), invoiceLineSample);
    LOGGER.info("Created line with id={}", secondLineId);

    verifyCollectionQuantity(INVOICE.getEndpoint(), 1);
    verifyCollectionQuantity(TestEntities.INVOICE_LINES.getEndpoint(), 2);

    // remove invoice
    deleteDataSuccess(INVOICE.getEndpointWithId(), invoiceSampleId);
    // verify invoice and associated invoice lines were deleted
    verifyCollectionQuantity(INVOICE.getEndpoint(), 0);
    verifyCollectionQuantity(TestEntities.INVOICE_LINES.getEndpoint(), 0);
  }

  @Test
  public void testCreateInvoiceNoDb() throws MalformedURLException {
    LOGGER.info(String.format("--- mod-invoice-storage %s test: Attempt to create invoice when no DB initialized", INVOICE.name()));
    String invoiceSample = getFile(INVOICE.getSampleFileName());
    JsonObject invoiceJson = new JsonObject(invoiceSample);
    invoiceJson.remove("id");
    invoiceSample = invoiceJson.toString();

    postData(INVOICE.getEndpoint(), invoiceSample, TENANT_WITHOUT_DB_HEADER)
      .then().log().ifValidationFails()
      .statusCode(500);
  }

  @Test
  public void testDeleteInvoiceNoDb() throws MalformedURLException {
    LOGGER.info(String.format("--- mod-invoice-storage %s test: Attempt to delete invoice when no DB initialized", INVOICE.name()));

    deleteData(INVOICE.getEndpointWithId(), UUID.randomUUID().toString(), TENANT_WITHOUT_DB_HEADER)
      .then().log().ifValidationFails()
      .statusCode(500);
  }
}
