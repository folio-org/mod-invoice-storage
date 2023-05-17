package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.folio.rest.utils.TestEntities.INVOICE_LINES;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;


public class InvoiceLineNumberTest extends TestBase {

  private static final Logger log = LogManager.getLogger(InvoiceLineNumberTest.class);

  private static final String INVOICE_LINE_NUMBER_ENDPOINT = "/invoice-storage/invoice-line-number";
  private static final String NON_EXISTING_INVOICE_ID = "f6b47acf-361a-497e-9ddb-45e3802df902";

  @Test
  public void testSequenceFlow() throws MalformedURLException {
    String invoiceId = null;
    try {

      log.info(String.format("--- mod-invoice-storage %s test: Creating an invoice with the next line number... ", INVOICE.name()));
      Invoice invoice = createInvoice();
      Assertions.assertEquals(1, invoice.getNextInvoiceLineNumber());
      invoiceId = invoice.getId();

      log.info(String.format("--- mod-invoice-storage %s test: Test retrieving invoice-line number for existing invoice and sequence ... ", INVOICE.name()));
      testGetInvoiceLineNumberForExistedIL(invoiceId);

      log.info(String.format("--- mod-invoice-storage %s test: Testing invoice-line numbers retrieving for non-existed invoice ID: %s", INVOICE.name(), NON_EXISTING_INVOICE_ID));
      testGetInvoiceLineNumberForNonExistedIL(NON_EXISTING_INVOICE_ID);

      log.info(String.format("--- mod-invoice-storage %s test: Testing invoice-line numbers retrieving based on existing invoice lines", INVOICE.name()));
      testGetInvoiceLineNumberBasedOnInvoiceLines(invoice);

    } finally {
      log.info(String.format("--- mod-invoice-storage %s test: Deleting %s with ID: %s", INVOICE.name(), INVOICE.name(), invoiceId));
      deleteDataSuccess(INVOICE.getEndpointWithId(), invoiceId);
    }
  }

  private Invoice createInvoice() throws MalformedURLException {
    JsonObject jsonSample = new JsonObject(getFile(INVOICE.getSampleFileName()));
    jsonSample.remove("id");
    String invoiceSample = jsonSample.encodePrettily();
    Response response = postData(INVOICE.getEndpoint(), invoiceSample);
    return response.then().extract().as(Invoice.class);
  }

  private InvoiceLine createInvoiceLine(String invoiceId) throws MalformedURLException {
    JsonObject jsonSample = new JsonObject(getFile(INVOICE_LINES.getSampleFileName()));
    jsonSample.remove("id");
    jsonSample.put("invoiceId", invoiceId);
    String invoiceLineSample = jsonSample.encodePrettily();
    Response response = postData(INVOICE_LINES.getEndpoint(), invoiceLineSample);
    return response.then().extract().as(InvoiceLine.class);
  }

  private void testGetInvoiceLineNumberForExistedIL(String invoiceId) throws MalformedURLException {
    int invoiceLineNumberInitial = retrieveInvoiceLineNumber(invoiceId);
    log.info("--- mod-invoice-storage test invoiceLineNumberInitial: " + invoiceLineNumberInitial);
    int i = 0; int numOfCalls = 2;
    while (i++ < numOfCalls) {
      log.info("--- mod-invoice-storage test Generate new sequence number: " + retrieveInvoiceLineNumber(invoiceId));
    }
    int invoiceLineNumberLast = retrieveInvoiceLineNumber(invoiceId);
    Assertions.assertEquals(i, invoiceLineNumberLast - invoiceLineNumberInitial);
  }

  @Test
  public void testGetInvoiceLineNumberWithInvalidCQLQuery() throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Invalid CQL query", "invoice-line-number"));
    testInvalidCQLQuery(INVOICE_LINE_NUMBER_ENDPOINT + "?query=invalid-query");
  }

  private void testGetInvoiceLineNumberForNonExistedIL(String invoiceId) throws MalformedURLException {
    Map<String, Object> params = new HashMap<>();
    params.put("invoiceId", invoiceId);
    getDataByParam(INVOICE_LINE_NUMBER_ENDPOINT, params)
      .then()
        .statusCode(404);
  }

  private int retrieveInvoiceLineNumber(String invoiceId) throws MalformedURLException {
    Map<String, Object> params = new HashMap<>();
    params.put("invoiceId", invoiceId);
    return Integer.parseInt(getDataByParam(INVOICE_LINE_NUMBER_ENDPOINT, params)
      .then()
        .statusCode(200)
        .extract()
        .response()
        .path("sequenceNumber"));
  }

  private void testGetInvoiceLineNumberBasedOnInvoiceLines(Invoice invoice) throws MalformedURLException {
    String invoiceId = invoice.getId();
    invoice.setNextInvoiceLineNumber(null);
    putData(INVOICE.getEndpointWithId(), invoiceId, JsonObject.mapFrom(invoice).encodePrettily())
      .then()
      .statusCode(204);
    InvoiceLine invoiceLine = createInvoiceLine(invoiceId);
    Assertions.assertEquals("1", invoiceLine.getInvoiceLineNumber());
    invoice.setNextInvoiceLineNumber(null);
    putData(INVOICE.getEndpointWithId(), invoiceId, JsonObject.mapFrom(invoice).encodePrettily())
      .then()
      .statusCode(204);
    int nextInvoiceLineNumber = retrieveInvoiceLineNumber(invoiceId);
    Assertions.assertEquals(2, nextInvoiceLineNumber);
  }
}
