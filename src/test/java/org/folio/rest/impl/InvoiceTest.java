package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICES;

import java.net.MalformedURLException;
import java.util.UUID;

import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class InvoiceTest extends TestBase {
  private final Logger logger = LoggerFactory.getLogger(InvoiceTest.class);

  @Test
  public void testDeleteInvoiceAndAssociatedLines() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Delete invoice and associated invoice lines", INVOICES.name()));
    String invoiceSample = getFile(INVOICES.getSampleFileName());
    JsonObject invoiceJson = new JsonObject(invoiceSample);
    invoiceJson.remove("id");
    invoiceSample = invoiceJson.toString();
    String invoiceSampleId = createEntity(INVOICES.getEndpoint(), invoiceSample);

    String invoiceLineSample = getFile(TestEntities.INVOICE_LINES.getSampleFileName());
    JsonObject invoiceLineJson = new JsonObject(invoiceLineSample);
    invoiceLineJson.remove("id");
    invoiceLineJson.put("invoiceId", invoiceSampleId);
    invoiceLineSample = invoiceLineJson.toString();

    String firstLineId = createEntity(TestEntities.INVOICE_LINES.getEndpoint(), invoiceLineSample);
    logger.info("Created line with id={}", firstLineId);
    String secondLineId = createEntity(TestEntities.INVOICE_LINES.getEndpoint(), invoiceLineSample);
    logger.info("Created line with id={}", secondLineId);

    verifyCollectionQuantity(INVOICES.getEndpoint(), 1);
    verifyCollectionQuantity(TestEntities.INVOICE_LINES.getEndpoint(), 2);

    // remove invoice
    deleteDataSuccess(INVOICES.getEndpointWithId(), invoiceSampleId);
    // verify invoice and associated invoice lines were deleted
    verifyCollectionQuantity(INVOICES.getEndpoint(), 0);
    verifyCollectionQuantity(TestEntities.INVOICE_LINES.getEndpoint(), 0);
  }

  @Test
  public void testCreateInvoiceNoDb() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Attempt to create invoice when no DB initialized", INVOICES.name()));
    String invoiceSample = getFile(INVOICES.getSampleFileName());
    JsonObject invoiceJson = new JsonObject(invoiceSample);
    invoiceJson.remove("id");
    invoiceSample = invoiceJson.toString();

    postData(INVOICES.getEndpoint(), invoiceSample, TENANT_WITHOUT_DB_HEADER)
      .then().log().ifValidationFails()
      .statusCode(500);
  }

  @Test
  public void testDeleteInvoiceNoDb() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Attempt to delete invoice when no DB initialized", INVOICES.name()));

    deleteData(INVOICES.getEndpointWithId(), UUID.randomUUID().toString(), TENANT_WITHOUT_DB_HEADER)
      .then().log().ifValidationFails()
      .statusCode(500);
  }
}
