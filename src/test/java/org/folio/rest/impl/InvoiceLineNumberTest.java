package org.folio.rest.impl;

import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import static org.folio.rest.utils.TestEntities.INVOICES;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.folio.rest.persist.PostgresClient;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException;

import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;

public class InvoiceLineNumberTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(InvoiceLineNumberTest.class);

  private static final String INVOICE_LINE_NUMBER_ENDPOINT = "/invoice-storage/invoice-line-number";
  private static final String SEQUENCE_ID = "\"ilNumber_8ad4b87b-9b47-4199-b0c3-5480745c6b41\"";
  private static final String NON_EXISTING_INVOICE_ID = "f6b47acf-361a-497e-9ddb-45e3802df902";
  
  private static final String CREATE_SEQUENCE = "CREATE SEQUENCE " + SEQUENCE_ID;
  private static final String SETVAL = "SELECT * FROM SETVAL('" + SEQUENCE_ID + "',13)";
  private static final String NEXTVAL = "SELECT * FROM NEXTVAL('" + SEQUENCE_ID + "')";
  private static final String DROP_SEQUENCE = "DROP SEQUENCE " + SEQUENCE_ID;

  @Test
  public void testSequenceFlow() throws MalformedURLException {
    String sampleId = null;
    try {
      
      logger.info(String.format("--- mod-invoice-storage %s test: Testing of environment on Sequence support", INVOICES.name()));
      testSequenceSupport();
      
      logger.info(String.format("--- mod-invoice-storage %s test: Creating an invoice and a sequence ... ", INVOICES.name()));
      JsonObject jsonSample = new JsonObject(getFile(INVOICES.getSampleFileName()));
      jsonSample.remove("id");
      String invoiceSample = jsonSample.encodePrettily();
      Response response = postData(INVOICES.getEndpoint(), invoiceSample);

      sampleId = response.then().extract().path("id");
      jsonSample.put("id", sampleId);
      logger.info(String.format("--- mod-invoice-storage %s test: Verify creating duplicate invoice fails", INVOICES.name()));
      testCreateDuplicateInvoice(jsonSample.encodePrettily());
      
      logger.info(String.format("--- mod-invoice-storage %s test: Test retrieving invoice-line number for existing invoice and sequence ... ", INVOICES.name()));

      testGetInvoiceLineNumberForExistedIL(sampleId);

      logger.info(String.format("--- mod-invoice-storage %s test: Testing invoice-line numbers retrieving for non-existed invoice ID: %s", INVOICES.name(), NON_EXISTING_INVOICE_ID));
      testGetInvoiceLineNumberForNonExistedIL(NON_EXISTING_INVOICE_ID);

      logger.info(String.format("--- mod-invoice-storage %s test: Update invoice with ID %s which will drop existing sequence", INVOICES.name(), sampleId));
      testInvoiceEdit(invoiceSample, sampleId);

      logger.info(String.format("--- mod-invoice-storage %s test: Verification/confirming of sequence deletion for invoice ID: %s",  INVOICES.name(), sampleId));
      testGetInvoiceLineNumberForNonExistedIL(sampleId);
      
      logger.info(String.format("--- mod-invoice-storage %s test: Test updating invoice with already deleted invoice-line number sequence", INVOICES.name()));
      testInvoiceEdit(invoiceSample, sampleId);

    } catch (Exception e) {
        logger.error(String.format("--- mod-invoice-storage test: %s API ERROR: %s", INVOICES.name(), e.getMessage()));
    } finally {
        logger.info(String.format("--- mod-invoice-storage %s test: Deleting %s with ID: %s", INVOICES.name(), INVOICES.name(), sampleId));
        deleteDataSuccess(INVOICES.getEndpointWithId(), sampleId);
    }
  }

  private void testCreateDuplicateInvoice(String invoiceSample) throws MalformedURLException {
    Response response = postData(INVOICES.getEndpoint(), invoiceSample);
    response.then()
      .statusCode(400)
      .body(Matchers.containsString("duplicate key value violates unique constraint \"" + INVOICE_TABLE + "_pkey\""));
  }
  
  private void testInvoiceEdit(String invoiceSample, String sampleId) throws MalformedURLException {
    JSONObject catJSON = new JSONObject(invoiceSample);
    catJSON.put("id", sampleId);
    catJSON.put("folioInvoiceNo", "666666");
    catJSON.put("status", "Cancelled");
    Response response = putInvoiceNumberData(sampleId, catJSON.toString());
    response.then()
      .statusCode(204);
  }
  
  private void testSequenceSupport() {
    execute(CREATE_SEQUENCE);
    execute(SETVAL);
    ResultSet rs = execute(NEXTVAL);
    execute(DROP_SEQUENCE);
    String result = rs.toJson().getJsonArray("results").getList().get(0).toString();
    assertEquals("[14]", result);
    try {
      execute(NEXTVAL);
    } catch (Exception e) {
        assertEquals(GenericDatabaseException.class, e.getCause().getClass());
    }
  }
  
  private void testGetInvoiceLineNumberForExistedIL(String invoiceId) throws MalformedURLException {
    int invoiceLineNumberInitial = retrieveInvoiceLineNumber(invoiceId);
    logger.info("--- mod-invoice-storage test invoiceLineNumberInitial: " + invoiceLineNumberInitial);
    int i = 0; int numOfCalls = 2;
    while (i++ < numOfCalls) {
    	logger.info("--- mod-invoice-storage test Generate new sequence number: " + retrieveInvoiceLineNumber(invoiceId));
    }
    int invoiceLineNumberLast = retrieveInvoiceLineNumber(invoiceId);
    assertEquals(i, invoiceLineNumberLast - invoiceLineNumberInitial);
  }

  @Test
  public void testGetInvoiceLineNumberWithInvalidCQLQuery() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Invalid CQL query", "invoice-line-number"));
    testInvalidCQLQuery(INVOICE_LINE_NUMBER_ENDPOINT + "?query=invalid-query");
  }
  
  private void testGetInvoiceLineNumberForNonExistedIL(String invoiceId) throws MalformedURLException {
    Map<String, Object> params = new HashMap<>();
    params.put("invoiceId", invoiceId);
    getDataByParam(INVOICE_LINE_NUMBER_ENDPOINT, params)
      .then()
        .statusCode(400);
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

  private static ResultSet execute(String query) {
    PostgresClient client = PostgresClient.getInstance(Vertx.vertx());
    CompletableFuture<ResultSet> future = new CompletableFuture<>();
    ResultSet resultSet = null;
    try {
      client.select(query, result -> {
        if (result.succeeded()) {
          future.complete(result.result());
        } else {
            future.completeExceptionally(result.cause());
        }
      });
      resultSet = future.get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
        future.completeExceptionally(e);
    }
    return resultSet;
  }
}
