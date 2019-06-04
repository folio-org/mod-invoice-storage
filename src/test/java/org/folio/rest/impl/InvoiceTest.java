package org.folio.rest.impl;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.utils.TestEntities;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.folio.rest.utils.TestEntities.INVOICE;

public class InvoiceTest extends TestBase {
  private final Logger logger = LoggerFactory.getLogger(InvoiceTest.class);

  @Test
  public void testDeleteInvoiceAndAssociatedLines() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Delete invoice and associated invoice lines", INVOICE.name()));
    String invoiceSample = getFile(INVOICE.getSampleFileName());
    JsonObject invoiceJson = new JsonObject(invoiceSample);
    invoiceJson.remove("id");
    invoiceSample = invoiceJson.toString();
    Response response = postData(INVOICE.getEndpoint(), invoiceSample);

    String invoiceSampleId = response.then().extract().path("id");
    String invoiceLineSample = getFile(TestEntities.INVOICE_LINES.getSampleFileName());
    JsonObject invoiceLineJson = new JsonObject(invoiceLineSample);
    invoiceLineJson.remove("id");
    invoiceLineJson.put("invoiceId", invoiceSampleId);
    invoiceLineSample = invoiceLineJson.toString();

    String firstLineId = createEntity(TestEntities.INVOICE_LINES.getEndpoint(), invoiceLineSample);
    logger.info("Created line with id={}", firstLineId);
    String secondLineId = createEntity(TestEntities.INVOICE_LINES.getEndpoint(), invoiceLineSample);
    logger.info("Created line with id={}", secondLineId);

    verifyCollectionQuantity(INVOICE.getEndpoint(), 2);
    verifyCollectionQuantity(TestEntities.INVOICE_LINES.getEndpoint(), 2);

    // remove invoice
    deleteDataSuccess(INVOICE.getEndpointWithId(), invoiceSampleId);
    // verify invoice and associated invoice lines were deleted
    verifyCollectionQuantity(INVOICE.getEndpoint(), INVOICE.getInitialQuantity());
    verifyCollectionQuantity(TestEntities.INVOICE_LINES.getEndpoint(), 0);
  }

}
