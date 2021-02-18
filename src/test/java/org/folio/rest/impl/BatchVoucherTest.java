package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.folio.rest.utils.TestEntities.BATCH_VOUCHER;
import static org.folio.rest.utils.TestEntities.BATCH_VOUCHER_EXPORTS;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

import java.net.MalformedURLException;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.utils.IsolatedTenant;
import org.folio.rest.utils.TestData.BatchVoucher;
import org.folio.rest.utils.TestData.BatchVoucherExports;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

public class BatchVoucherTest extends TestBase {
  public static final String BATCH_VOUCHER_ENDPOINT = "/batch-voucher-storage/batch-vouchers";
  public static final String BATCH_VOUCHER_ENDPOINT_WITH_ID = "/batch-voucher-storage/batch-vouchers/{id}";
  public static final String PATH_TEST_BATCH_VOUCHER = "data/batch-vouchers/test-batch-voucher.json";
  private static final String NONEXISTENT_VOUCHER_ID = "12345678-83b9-1234-9c39-b58dcd02ee10";
  private static JsonObject BATCH_VOUCHER_WITH_ID;
  private static JsonObject BATCH_VOUCHER_WITHOUT_ID;
  private static final String BAD_REQUEST = "";

  @BeforeAll
  public static void beforeAll() {
    BATCH_VOUCHER_WITH_ID = createBatchVoucherJSON(PATH_TEST_BATCH_VOUCHER, false);
    BATCH_VOUCHER_WITHOUT_ID = createBatchVoucherJSON(PATH_TEST_BATCH_VOUCHER, true);
  }

  @Test
  public void testPostShouldCreateBatchVoucherIfRequestBodyIsCorrectAndIdProvided() throws MalformedURLException {
    Response createdBatchVoucher =
        given()
            .spec(commonRequestSpec())
            .body(BATCH_VOUCHER_WITH_ID.toString())
        .when()
            .post(storageUrl(BATCH_VOUCHER_ENDPOINT))
        .then()
            .assertThat()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body(ID, equalTo(BATCH_VOUCHER_WITH_ID.getString(ID)))
            .extract()
            .response();
    assertAllFieldsExistAndEqual(BATCH_VOUCHER_WITH_ID, createdBatchVoucher);
  }

  @Test
  public void testPostShouldCreateBatchVoucherIfRequestBodyIsCorrectAndIdIsNotProvided() throws MalformedURLException {
    Response createdBatchVoucher =
        given()
            .spec(commonRequestSpec())
            .body(BATCH_VOUCHER_WITHOUT_ID.toString())
        .when()
            .post(storageUrl(BATCH_VOUCHER_ENDPOINT))
      .then()
          .assertThat()
          .statusCode(201)
          .contentType(ContentType.JSON)
          .body(ID, notNullValue())
          .extract()
          .response();
    assertAllFieldsExistAndEqual(BATCH_VOUCHER_WITHOUT_ID, createdBatchVoucher);
  }

  @Test
  public void testPostShouldReturn400JSONErrorIfRequestIsIncorrect() throws MalformedURLException {
    given()
        .spec(commonRequestSpec())
        .body(BAD_REQUEST)
    .when()
         .post(storageUrl(BATCH_VOUCHER_ENDPOINT))
    .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void testGetShouldReturnBatchVoucherById() throws MalformedURLException {
    String expBatchVoucherId = postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    Response returnedBatchVoucher =
      given()
          .spec(commonRequestSpec())
          .pathParam(ID, expBatchVoucherId)
      .when()
          .get(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID))
      .then()
          .assertThat()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .extract()
          .response();
    assertAllFieldsExistAndEqual(BATCH_VOUCHER_WITHOUT_ID, returnedBatchVoucher);
  }

  @Test
  public void testGetShouldReturn404IfVoucherWithProvidedIdIsAbsent() throws MalformedURLException {
    postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given()
        .spec(commonRequestSpec())
        .pathParam(ID, NONEXISTENT_VOUCHER_ID)
    .when()
        .get(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID))
    .then()
        .assertThat()
        .statusCode(404);
  }

  @Test
  public void testGetShouldReturn422IfProvidedIdHasIncorrectFormat() throws MalformedURLException {
    postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given()
        .spec(commonRequestSpec())
        .pathParam(ID, "38a8c92-9f45-4a86-98c3-76a20f1615ee")
    .when()
        .get(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID))
    .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void testDeleteShouldDeleteVoucherByProvidedId() throws MalformedURLException {
    String expBatchVoucherId = postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given()
        .spec(commonRequestSpec())
        .pathParam(ID, expBatchVoucherId)
    .when()
        .delete(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID))
    .then()
        .assertThat()
        .statusCode(204);
  }


  @Test
  @IsolatedTenant
  public void testDeleteShouldDeleteVoucherAndRelatedExportsByProvidedId() throws MalformedURLException {
    givenTestData(Pair.of(BATCH_VOUCHER, BatchVoucher.DEFAULT),
                  Pair.of(BATCH_VOUCHER_EXPORTS, BatchVoucherExports.DEFAULT));

    // Check that BatchVoucher has been created
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER.getId())
    .when()
      .get(storageUrl(BATCH_VOUCHER.getEndpointWithId()))
    .then()
      .assertThat()
      .statusCode(200);

    // Delete BatchVoucher by id
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER.getId())
    .when()
      .delete(storageUrl(BATCH_VOUCHER.getEndpointWithId()))
    .then()
      .assertThat()
      .statusCode(204);

    // Check that BatchVoucher has been deleted
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER.getId())
    .when()
      .get(storageUrl(BATCH_VOUCHER.getEndpointWithId()))
    .then()
      .assertThat()
      .statusCode(404);

    // Check that BatchVoucherExports has been deleted
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER_EXPORTS.getId())
      .when()
      .get(storageUrl(BATCH_VOUCHER_EXPORTS.getEndpointWithId()))
      .then()
      .assertThat()
      .statusCode(404);
  }

  @Test
  public void testDeleteShouldReturn404IfVoucherWithProvidedIdIsAbsent() throws MalformedURLException {
    postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given()
        .spec(commonRequestSpec())
        .pathParam(ID, NONEXISTENT_VOUCHER_ID)
    .when()
        .delete(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID))
    .then()
        .assertThat()
        .statusCode(404);
  }

  private static JsonObject createBatchVoucherJSON(String path, boolean deleteId) {
    String invoiceSample = getFile(path);
    JsonObject batchVoucher = new JsonObject(invoiceSample);
    if (deleteId) {
      batchVoucher.remove(ID);
    }
    return batchVoucher;
  }
}
