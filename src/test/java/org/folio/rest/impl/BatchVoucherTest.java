package org.folio.rest.impl;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;


public class BatchVoucherTest extends TestBase {
  private static final String ID = "id";
  private static final String BATCH_VOUCHER_ENDPOINT = "/batch-voucher-storage/batch-vouchers";
  public static final String PATH_VOUCHER_WITHOUT_ID = "data/vouchers/correctBatchVoucherWithoutId.json";
  public static final String PATH_VOUCHER_WITH_ID = "data/vouchers/correctBatchVoucher.json";

  @Test
  public void testPost201_Should_CreateBatchVoucher_IfRequestBodyIsCorrect_And_IdProvided() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITH_ID);

    postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString())
      .then().log().ifValidationFails()
      .assertThat()
        .statusCode(201)
        .body(ID, equalTo(batchVoucher.getString(ID)));
  }

  @Test
  public void testPost201_Should_CreateBatchVoucher_IfRequestBodyIsCorrect_And_IdIsNotProvided() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITHOUT_ID);
    postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString())
      .then().log().ifValidationFails().assertThat()
        .statusCode(201)
        .body(ID, notNullValue());
  }

  @Test
  public void testGet200_Should_ReturnBatchVoucher_ById() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITHOUT_ID);
    String expBatchVoucherId = postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString()).path(ID);

    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .pathParam("batchVoucherId", expBatchVoucherId).
    when().
        get(storageUrl(BATCH_VOUCHER_ENDPOINT+"/{batchVoucherId}")).
    then().assertThat().
      statusCode(200);
  }

  @Test
  public void testGet404_Should_Return_IfVoucherWithProvidedIdIsAbsent() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITHOUT_ID);
    postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString());

    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .pathParam("batchVoucherId", "12345678-83b9-1234-9c39-b58dcd02ee10").
    when().
      get(storageUrl(BATCH_VOUCHER_ENDPOINT+"/{batchVoucherId}")).
    then().assertThat().
      statusCode(404);
  }

  @Test
  public void testGet422_Should_Return_IfProvidedIdHasIncorrectFormat() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITHOUT_ID);
    postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString());

    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .pathParam("batchVoucherId", "test-83b9-1234-9c39-b58dcd02ee10").
    when().
      get(storageUrl(BATCH_VOUCHER_ENDPOINT+"/{batchVoucherId}")).
    then().assertThat().
      statusCode(422);
  }

  @Test
  public void testDelete204_Should_DeleteVoucher_ByProvidedId() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITHOUT_ID);
    String expBatchVoucherId =  postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString()).path(ID);

    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .pathParam("batchVoucherId", expBatchVoucherId).
    when().
      delete(storageUrl(BATCH_VOUCHER_ENDPOINT+"/{batchVoucherId}")).
    then().assertThat().
      statusCode(204);
  }

  @Test
  public void testDelete404_Should_Return_IfVoucherWithProvidedIdIsAbsent() throws MalformedURLException {
    JsonObject batchVoucher = createBatchVoucherJSON(PATH_VOUCHER_WITHOUT_ID);
    postData(BATCH_VOUCHER_ENDPOINT, batchVoucher.toString());

    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .pathParam("batchVoucherId", "12345678-83b9-1234-9c39-b58dcd02ee10").
    when().
      delete(storageUrl(BATCH_VOUCHER_ENDPOINT+"/{batchVoucherId}")).
    then().assertThat().
      statusCode(404);
  }

  @NotNull
  private JsonObject createBatchVoucherJSON(String path) {
    String invoiceSample = getFile(path);
    return new JsonObject(invoiceSample);
  }
}
