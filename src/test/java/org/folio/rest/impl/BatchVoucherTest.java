package org.folio.rest.impl;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.net.MalformedURLException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

public class BatchVoucherTest extends TestBase {
  public static final String BATCH_VOUCHER_ENDPOINT = "/batch-voucher-storage/batch-vouchers";
  public static final String BATCH_VOUCHER_ENDPOINT_WITH_ID = "/batch-voucher-storage/batch-vouchers/{id}";
  public static final String PATH_TEST_BATCH_VOUCHER = "data/batch-vouchers/test-batch-voucher.json";
  private static final String NONEXISTENT_VOUCHER_ID = "12345678-83b9-1234-9c39-b58dcd02ee10";
  private static JsonObject BATCH_VOUCHER_WITH_ID;
  private static JsonObject BATCH_VOUCHER_WITHOUT_ID;
  private static String BAD_REQUEST = "";

  @BeforeAll
  public static void beforeAll()  {
    BATCH_VOUCHER_WITH_ID = createBatchVoucherJSON(PATH_TEST_BATCH_VOUCHER, false);
    BATCH_VOUCHER_WITHOUT_ID = createBatchVoucherJSON(PATH_TEST_BATCH_VOUCHER, true);
  }

  @Test
  public void testPostShouldCreateBatchVoucherIfRequestBodyIsCorrectAndIdProvided() throws MalformedURLException {
    Response createdBatchVoucher = given().
                                        spec(commonRequestSpec()).
                                        body(BATCH_VOUCHER_WITH_ID.toString()).
                                   when().
                                        post(storageUrl(BATCH_VOUCHER_ENDPOINT)).
                                   then().
                                        assertThat().
                                        statusCode(201).
                                        contentType(ContentType.JSON).
                                        body(ID, equalTo(BATCH_VOUCHER_WITH_ID.getString(ID))).
                                        extract().
                                        response();
    assertAllFieldsExistAndEqual(BATCH_VOUCHER_WITH_ID, createdBatchVoucher);
  }

  @Test
  public void testPostShouldCreateBatchVoucherIfRequestBodyIsCorrectAndIdIsNotProvided() throws MalformedURLException {
    Response createdBatchVoucher = given().
                                        spec(commonRequestSpec()).
                                        body(BATCH_VOUCHER_WITHOUT_ID.toString()).
                                   when().
                                        post(storageUrl(BATCH_VOUCHER_ENDPOINT)).
                                   then().
                                        assertThat().
                                        statusCode(201).
                                        contentType(ContentType.JSON).
                                        body(ID, notNullValue()).
                                        extract().
                                        response();
    assertAllFieldsExistAndEqual(BATCH_VOUCHER_WITHOUT_ID, createdBatchVoucher);
  }

  @Test
  public void testPostShouldReturn400JSONErrorIfRequestIsIncorrect() throws MalformedURLException {
    given().
        spec(commonRequestSpec()).
        body(BAD_REQUEST).
    when().
        post(storageUrl(BATCH_VOUCHER_ENDPOINT)).
    then().
        assertThat().
        statusCode(400);
  }

  @Test
  public void testGetShouldReturnBatchVoucherById() throws MalformedURLException {
    String expBatchVoucherId = postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    Response returnedBatchVoucher = given().
                                        spec(commonRequestSpec()).
                                        pathParam(ID, expBatchVoucherId).
                                    when().
                                        get(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID)).
                                    then().assertThat().
                                        statusCode(200).
                                        extract().
                                        response();
    assertAllFieldsExistAndEqual(BATCH_VOUCHER_WITHOUT_ID, returnedBatchVoucher);
  }

  @Test
  public void testGetShouldReturn404IfVoucherWithProvidedIdIsAbsent() throws MalformedURLException {
    postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given().
        spec(commonRequestSpec()).
        pathParam(ID, NONEXISTENT_VOUCHER_ID).
    when().
        get(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID)).
    then().
        assertThat().
        statusCode(404);
  }

  @Test
  public void testGetShouldReturn422IfProvidedIdHasIncorrectFormat() throws MalformedURLException {
    postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given().
        spec(commonRequestSpec()).
        pathParam(ID, "test-83b9-1234-9c39-b58dcd02ee10").
    when().
        get(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID)).
    then().assertThat().
        statusCode(422);
  }

  @Test
  public void testDeleteShouldDeleteVoucherByProvidedId() throws MalformedURLException {
    String expBatchVoucherId =  postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given().
        spec(commonRequestSpec()).
        pathParam(ID, expBatchVoucherId).
    when().
        delete(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID)).
    then().assertThat().
        statusCode(204);
  }

  @Test
  public void testDeleteShouldReturn404IfVoucherWithProvidedIdIsAbsent() throws MalformedURLException {
    postData(BATCH_VOUCHER_ENDPOINT, BATCH_VOUCHER_WITHOUT_ID.toString()).path(ID);

    given().
        spec(commonRequestSpec()).
        pathParam(ID, NONEXISTENT_VOUCHER_ID).
    when().
        delete(storageUrl(BATCH_VOUCHER_ENDPOINT_WITH_ID)).
    then().assertThat().
        statusCode(404);
  }

  private static JsonObject createBatchVoucherJSON(String path, boolean deleteId) {
    String invoiceSample = getFile(path);
    JsonObject batchVoucher = new JsonObject(invoiceSample);
    if (deleteId){
      batchVoucher.remove(ID);
    }
    return batchVoucher;
  }
}
