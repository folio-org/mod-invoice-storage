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

public class VoucherNumberTest extends TestBase {

  @Rule
  public RepeatRule rule = new RepeatRule();
  private static List<Long> voucherNumberList;

  private static final String SEQUENCE_NUMBER = "sequenceNumber";
  private static final String VOUCHER_NUMBER_ENDPOINT = "/voucher-storage/voucher-number";
  private static final String VOUCHER_STORAGE_VOUCHER_NUMBER_START_ENDPOINT = "/voucher-storage/voucher-number/start";
  private static final String VOUCHER_NUMBER_INVALID_START_ENDPOINT = "/voucher-storage/voucher-number/bad_start";
  private static final String DROP_SEQUENCE_QUERY = "DROP SEQUENCE diku_mod_invoice_storage.voucher_number";
  private static final String RESET_START_SEQUENCE_QUERY = "ALTER SEQUENCE diku_mod_invoice_storage.voucher_number START WITH 105";
  

  @BeforeClass
  public static void setUp() {
    voucherNumberList  = new ArrayList<>();
  }

  @Test
  @Repeat(3)
  public void testGetVoucherNumber() throws MalformedURLException {
    voucherNumberList.add(getNumberAsLong());
  }

  @Test
  public void testCurrentStartValueVoucherNumber() throws Exception {
    // Get and verify Voucher number's start value defaults to 0
    assertThat(getCurrentStartValueVoucherNumber(), equalTo(0L));
  }

  @Test
  public void testResetedStartValueVoucherNumber() throws Exception {
    // reset start value in sequence
    resetStartSequenceInDb();
    // verify current start value equals new reseted start value
    assertThat(getCurrentStartValueVoucherNumber(), equalTo(105L));
  }

  @Test
  public void testCurrentStartValueVoucherNumberInvalidUrl() throws MalformedURLException {
    getData(VOUCHER_NUMBER_INVALID_START_ENDPOINT).then()
      .statusCode(400);
  }

  @AfterClass
  public static void tearDown() throws Exception {

    // Verify expected start value
    assertThat(voucherNumberList.get(0), equalTo(0L));
    
    // Positive scenario - testing of number increase
    for(int i = 0; i < voucherNumberList.size(); i++) {
      assertThat(voucherNumberList.get(i) - voucherNumberList.get(0), equalTo((long) i));
    }

    // Negative scenario - retrieving number from non-existed sequence
    dropSequenceInDb();
    testProcessingErrorReply();
  }

  private long getCurrentStartValueVoucherNumber() throws MalformedURLException {
    return new Long(getData(VOUCHER_STORAGE_VOUCHER_NUMBER_START_ENDPOINT)
      .then()
        .statusCode(HttpStatus.HTTP_OK.toInt())
        .extract()
          .response()
            .path(SEQUENCE_NUMBER));
  }

  private long getNumberAsLong() throws MalformedURLException {
    return new Long(getData(VOUCHER_NUMBER_ENDPOINT)
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
        .get(storageUrl(VOUCHER_NUMBER_ENDPOINT))
          .then()
            .statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
            .contentType(TEXT_PLAIN)
            .extract()
              .response();
  }

  private static void resetStartSequenceInDb() throws Exception {
    CompletableFuture<UpdateResult> future = new CompletableFuture<>();
      PostgresClient.getInstance(Vertx.vertx()).execute(RESET_START_SEQUENCE_QUERY, result -> {
        if(result.failed()) {
          future.completeExceptionally(result.cause());
        } else {
          future.complete(result.result());
        }
      });
      future.get(10, TimeUnit.SECONDS);
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
