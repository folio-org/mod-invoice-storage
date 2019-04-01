package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.folio.rest.persist.PostgresClient;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException;

import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class InvoiceLineNumberTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(InvoiceLineNumberTest.class);

  private static final String INVOICE_ENDPOINT = "/invoice-storage/invoices";
  private static final String INVOICE_LINE_NUMBER_ENDPOINT = "/invoice-storage/invoice-line-number";
  private static final String SEQUENCE_ID = "\"ilNumber_8ad4b87b-9b47-4199-b0c3-5480745c6b41\"";

  private static final String CREATE_SEQUENCE = "CREATE SEQUENCE " + SEQUENCE_ID;
  private static final String SETVAL = "SELECT * FROM SETVAL('" + SEQUENCE_ID + "',13)";
  private static final String NEXTVAL = "SELECT * FROM NEXTVAL('" + SEQUENCE_ID + "')";
  private static final String DROP_SEQUENCE = "DROP SEQUENCE " + SEQUENCE_ID;

  @Test
  public void testSequenceFlow() throws MalformedURLException {
    String sampleId = null;
    try {
      logger.info("--- mod-invoice-storage invoice test: Testing of environment on Sequence support");
      testSequenceSupport();

      logger.info("--- mod-invoice-storage invoice test: Creating invoice/invoice-line number sequence ... ");
      String invoiceSample = getFile(INVOICE.getSampleFileName());
      Response response = postData(INVOICE.getEndpoint(), invoiceSample);
      logger.info("--- mod-invoice-storage response: " + response.getBody().prettyPrint());

      logger.info("--- mod-invoice-storage invoice test: Testing invoice-line numbers retrieving for existed invoice ... ");
      sampleId = response.then().extract().path("id");
      testGetInvoiceLineNumberForExistedIL(sampleId);

      logger.info("--- mod-invoice-storage invoice test: Testing invoice-line numbers retrieving for non-existed invoice ... ");
      testGetInvoiceLineNumberForNonExistedIL("non-existed-invoice-id");

      logger.info("--- mod-invoice-storage invoice test: Editing invoice with ID: " + sampleId);
      testInvoiceEdit(invoiceSample, sampleId);

      logger.info("--- mod-invoice-storage invoice test: Verification/confirming of sequence deletion ...");
      testGetInvoiceLineNumberForNonExistedIL(sampleId);

      logger.info("--- mod-invoice-storage invoice test: Testing updated invoice with already deleted POL numbers sequence ...");
      testInvoiceEdit(invoiceSample, sampleId);

    } catch (Exception e) {
      logger.error(String.format("--- mod-invoice-storage-test: %s API ERROR: %s", INVOICE.name(), e.getMessage()));
    }  finally {
      logger.info(String.format("--- mod-invoice-storage %s test: Deleting %s with ID: %s", INVOICE.name(), INVOICE.name(), sampleId));
      deleteDataSuccess(INVOICE.getEndpointWithId(), sampleId);
    }
  }

  private void testInvoiceEdit(String invoiceSample, String sampleId) throws MalformedURLException {
    JSONObject catJSON = new JSONObject(invoiceSample);
    catJSON.put("id", sampleId);
    catJSON.put("folioInvoiceNo", "666666");
    catJSON.put("status", "Cancelled");
    Response response = putInvoiceNumberData(INVOICE_ENDPOINT, sampleId, catJSON.toString());
    response.then()
      .statusCode(204);
  }

  private void testSequenceSupport() {
    execute(CREATE_SEQUENCE);
    execute(SETVAL);
    ResultSet rs = execute(NEXTVAL);
    execute(DROP_SEQUENCE);
    String result = rs.toJson().getJsonArray("results").getList().get(0).toString();
    //logger.info("--- mod-invoice-storage result test: result: " + rs.toJson().encodePrettily());
    assertEquals("[14]", result);
    try {
      execute(NEXTVAL);
    } catch(Exception e) {
      assertEquals(GenericDatabaseException.class, e.getCause().getClass());
    }
  }
  
  private void testGetInvoiceLineNumberForExistedIL(String invoiceId) throws MalformedURLException {
    int invoiceLineNumberInitial = retrieveInvoiceLineNumber(invoiceId);
    logger.info("--- mod-invoice-storage invoiceLineNumberInitial: " + invoiceLineNumberInitial);
    int i = 0; int numOfCalls = 2;
    while(i++ < numOfCalls) {
    	logger.info("--- mod-invoice-storage retrieveInvoiceLineNumber(invoiceId) : " + retrieveInvoiceLineNumber(invoiceId));
    }
    int invoiceLineNumberLast = retrieveInvoiceLineNumber(invoiceId);
    logger.info("--- mod-invoice-storage invoiceLineNumberLast - invoiceLineNumberInitial: " + invoiceLineNumberLast + "-" + invoiceLineNumberInitial);
    assertEquals(i, invoiceLineNumberLast - invoiceLineNumberInitial);
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
    //logger.info("--- mod-invoice-storage retrieveInvoiceLineNumber(invoiceId) : " + res.getBody().prettyPrint());
  }

  private static ResultSet execute(String query) {
    PostgresClient client = PostgresClient.getInstance(Vertx.vertx());
    CompletableFuture<ResultSet> future = new CompletableFuture<>();
    ResultSet resultSet = null;
    try {
      client.select(query, result -> {
        if(result.succeeded()) {
          future.complete(result.result());
        }
        else {
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
