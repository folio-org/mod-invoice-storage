package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.folio.HttpStatus;
import org.folio.rest.persist.PostgresClient;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
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
      
      logger.info("--- mod-invoice-storage invoice test: Creating invoice and a sequence ... ");
      String invoiceSample = getFile(INVOICE.getSampleFileName());
      Response response = postData(INVOICE.getEndpoint(), invoiceSample);
      logger.info("--- mod-invoice-storage response: " + response.getBody().prettyPrint());
      
      logger.info(String.format("--- mod-invoice-storage test: Verify creating duplicate invoice fails"));
      testCreateDuplicateInvoice(invoiceSample);
      
      logger.info("--- mod-invoice-storage invoice test: Test retrieving invoice-line number for existed invoice and sequence ... ");
      sampleId = response.then().extract().path("id");
      testGetInvoiceLineNumberForExistedIL(sampleId);

      logger.info("--- mod-invoice-storage invoice test: Testing invoice-line numbers retrieving for non-existed invoice ... ");
      testGetInvoiceLineNumberForNonExistedIL("non-existed-invoice-id");

      logger.info("--- mod-invoice-storage invoice test: Update invoice with ID which will drop existing sequence : " + sampleId);
      testInvoiceEdit(invoiceSample, sampleId);

      logger.info("--- mod-invoice-storage invoice test: Verification/confirming of sequence deletion ...");
      testGetInvoiceLineNumberForNonExistedIL(sampleId);
      
      logger.info("--- mod-invoice-storage invoice test: Test updating invoice with already deleted invoice-line numbers sequence ...");
      testInvoiceEdit(invoiceSample, sampleId);

    } catch (Exception e) {
        logger.error(String.format("--- mod-invoice-storage-test: %s API ERROR: %s", INVOICE.name(), e.getMessage()));
    } finally {
        logger.info(String.format("--- mod-invoice-storage %s test: Deleting %s with ID: %s", INVOICE.name(), INVOICE.name(), sampleId));
        deleteDataSuccess(INVOICE.getEndpointWithId(), sampleId);
    }
  }

  @Test
  public void testDropSequence() throws MalformedURLException {
    try {
    	
      logger.info("--- mod-invoice-storage test deleteSequenceInDb: Dropping sequence in DB ... ");
      dropSequenceInDb();

      logger.info("--- mod-invoice-storage test testProcessingErrorReply: Failure to get sequence number ... ");
      testProcessingErrorReply();
      
    } catch (Exception e) {
        logger.error(String.format("--- mod-invoice-storage-test: %s API ERROR: %s", INVOICE.name(), e.getMessage()));
    }
  }
  
  private void dropSequenceInDb() throws Exception {
    CompletableFuture<UpdateResult> future = new CompletableFuture<>();
    PostgresClient.getInstance(Vertx.vertx()).execute(DROP_SEQUENCE, result -> {
      if(result.failed()) {
        future.completeExceptionally(result.cause());
      } else {
        future.complete(result.result());
      }
    });
    future.get(10, TimeUnit.SECONDS);
  }
  
  private void testProcessingErrorReply() throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
        .get(storageUrl(INVOICE_LINE_NUMBER_ENDPOINT))
          .then()
          .statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
          .contentType(TEXT_PLAIN)
          .extract()
          .response();
  }
  
  public void testCreateDuplicateInvoice(String invoiceSample) throws MalformedURLException {
    Response response = postData(INVOICE.getEndpoint(), invoiceSample);
    response.then()
      .statusCode(400);
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
    	logger.info("--- mod-invoice-storage Generate new sequence number: " + retrieveInvoiceLineNumber(invoiceId));
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
