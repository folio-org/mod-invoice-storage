package org.folio.rest.impl;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.Vertx;
import io.vertx.ext.sql.UpdateResult;
import org.folio.HttpStatus;
import org.folio.rest.persist.PostgresClient;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VoucherNumberTest extends TestBase {

  private static final String VALUE = "value";
  private static List<Long> voucherNumberList = new ArrayList<>();

  private static final String SEQUENCE_NUMBER = "sequenceNumber";
  private static final String VOUCHER_NUMBER_ENDPOINT = "/voucher-storage/voucher-number";
  private static final String VOUCHER_STORAGE_VOUCHER_NUMBER_START_ENDPOINT = "/voucher-storage/voucher-number/start/";
  private static final String VOUCHER_NUMBER_INVALID_START_ENDPOINT = "/voucher-storage/voucher-number/bad_start";
  private static final String DROP_SEQUENCE_QUERY = "DROP SEQUENCE diku_mod_invoice_storage.voucher_number";
  private static final int NUM_OF_REQUESTS = 3;

  @Test
  public void testGetVoucherNumber() throws Exception {

    // Retrieve voucher numbers
    for(int i = 0; i < NUM_OF_REQUESTS; i++) {
      voucherNumberList.add(getSequenceNumberValue());
    }

    // Verify expected start value
    assertThat(voucherNumberList.get(0), equalTo(0L));

    // Positive scenario - testing of number increase
    for(int i = 0; i < voucherNumberList.size(); i++) {
      assertThat(voucherNumberList.get(i) - voucherNumberList.get(0), equalTo((long) i));
    }

    assertThat(getCurrentStartValueVoucherNumber(), equalTo(0L));

    // Verify changing of start value
    long start = 11111111L;

    changeStartValueResponse(start)
      .statusCode(HttpStatus.HTTP_NO_CONTENT.toInt());

    // verify current start value equals new reseted start value 
    assertThat(getCurrentStartValueVoucherNumber(), equalTo(start));
    
    assertThat(getSequenceNumberValue(), is(start));

    // Negative scenario - changing start value with bad request
    changeStartValueResponse(10.1)
      .statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
      .contentType(ContentType.TEXT);

    // Negative scenario - retrieving voucher number and changing start value from non-existed (deleted) sequence
    dropSequenceInDb();

    getSequenceNumberResponse()
      .statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
      .contentType(ContentType.TEXT);

    changeStartValueResponse(100)
      .statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt())
      .contentType(ContentType.TEXT);
  }
  
  @Test
  public void testCurrentStartValueVoucherNumberInvalidUrl() throws MalformedURLException {
    getData(VOUCHER_NUMBER_INVALID_START_ENDPOINT).then()
      .statusCode(400);
  }
  
  private long getCurrentStartValueVoucherNumber() throws MalformedURLException {
    return new Long(getData(VOUCHER_STORAGE_VOUCHER_NUMBER_START_ENDPOINT)
      .then()
        .statusCode(HttpStatus.HTTP_OK.toInt())
        .extract()
          .response()
            .path(SEQUENCE_NUMBER));
  }
  
  private ValidatableResponse getSequenceNumberResponse() throws MalformedURLException {
    return given()
      .header(TENANT_HEADER)
        .when()
          .get(storageUrl(VOUCHER_NUMBER_ENDPOINT))
        .then();
  }

  private ValidatableResponse changeStartValueResponse(Object value) throws MalformedURLException {
    return given()
      .header(TENANT_HEADER)
      .pathParam(VALUE, value)
        .when()
          .post(storageUrl(VOUCHER_STORAGE_VOUCHER_NUMBER_START_ENDPOINT + "{value}"))
        .then();
  }

  private long getSequenceNumberValue() throws MalformedURLException {
    return Long.parseLong(
      getSequenceNumberResponse()
        .statusCode(HttpStatus.HTTP_OK.toInt())
        .extract()
          .response()
            .path(SEQUENCE_NUMBER));
  }

  private void dropSequenceInDb() throws Exception {
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
