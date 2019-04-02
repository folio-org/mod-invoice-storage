package org.folio.rest.impl;

import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.unit.junit.Repeat;
import io.vertx.ext.unit.junit.RepeatRule;
import org.folio.HttpStatus;
import org.folio.rest.persist.PostgresClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class InvoiceNumberTest extends TestBase {

  @Rule
  public RepeatRule rule = new RepeatRule();
  private static List<Long> invoiceNumberList;

  private static final String SEQUENCE_NUMBER = "sequenceNumber";
  private static final String INVOICE_NUMBER_ENDPOINT = "/invoice-storage/invoice-number";
  private static final String DROP_SEQUENCE_QUERY = "DROP SEQUENCE diku_mod_invoice_storage.invoice_number";

  @BeforeClass
  public static void setUp() {
    invoiceNumberList  = new ArrayList<>();
  }

  @Test
  @Repeat(3)
  public void testGetInvoiceNumber() throws MalformedURLException {
    invoiceNumberList.add(getNumberAsLong());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    assertThat(invoiceNumberList.get(1) - invoiceNumberList.get(0), equalTo(1L));
    assertThat(invoiceNumberList.get(2) - invoiceNumberList.get(0), equalTo(2L));

    // Negative scenario - retrieving number from non-existed sequence
    dropSequenceInDb();
    testProcessingErrorReply();
  }

  private long getNumberAsLong() throws MalformedURLException {
    return new Long(getData(INVOICE_NUMBER_ENDPOINT)
      .then()
        .statusCode(HttpStatus.HTTP_OK.toInt())
        .extract()
          .response()
            .path(SEQUENCE_NUMBER));
  }

  private static void testProcessingErrorReply() throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
        .get(storageUrl(INVOICE_NUMBER_ENDPOINT))
          .then()
            .statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
            .contentType(TEXT_PLAIN)
            .extract()
              .response();
  }

  private static void dropSequenceInDb() throws Exception {
    CompletableFuture<UpdateResult> future = new CompletableFuture<>();
      PostgresClient.getInstance(Vertx.vertx()).execute(DROP_SEQUENCE_QUERY, result -> {
        if(result.failed()) {
          future.completeExceptionally(result.cause());
        } else {
          future.complete(result.result());
        }
      });
      future.get(10, TimeUnit.SECONDS);
  }
}
