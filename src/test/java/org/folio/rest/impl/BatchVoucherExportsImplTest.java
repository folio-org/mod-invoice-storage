package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.folio.rest.utils.TestEntities.BATCH_GROUP;
import static org.folio.rest.utils.TestEntities.BATCH_VOUCHER;
import static org.folio.rest.utils.TestEntities.BATCH_VOUCHER_EXPORTS;

import java.net.MalformedURLException;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.utils.IsolatedTenant;
import org.folio.rest.utils.TestData;
import org.junit.jupiter.api.Test;

class BatchVoucherExportsImplTest extends TestBase {

  @Test
  @IsolatedTenant
  public void testDeleteShouldDeleteExportsByProvidedIdAndRelatedBatchVoucher() throws MalformedURLException {
    givenTestData(Pair.of(BATCH_GROUP, TestData.BatchGroup.DEFAULT),
      Pair.of(BATCH_VOUCHER, TestData.BatchVoucher.DEFAULT),
      Pair.of(BATCH_VOUCHER_EXPORTS, TestData.BatchVoucherExports.DEFAULT));

    // Check that Batch Voucher Exports has been created
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER_EXPORTS.getId())
    .when()
      .get(storageUrl(BATCH_VOUCHER_EXPORTS.getEndpointWithId()))
    .then()
      .assertThat()
      .statusCode(200);

    // Delete Batch Voucher Exports by id
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER_EXPORTS.getId())
    .when()
      .delete(storageUrl(BATCH_VOUCHER_EXPORTS.getEndpointWithId()))
    .then()
      .assertThat()
      .statusCode(204);

    // Check that BatchVoucherExports has been deleted
    given()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER_EXPORTS.getId())
      .when()
      .get(storageUrl(BATCH_VOUCHER_EXPORTS.getEndpointWithId()))
      .then()
      .assertThat()
      .statusCode(404);

    // Check that BatchVoucher has been deleted
    given().log().all()
      .spec(isolatedRequestSpec())
      .pathParam(ID, BATCH_VOUCHER.getId())
      .when()
      .get(storageUrl(BATCH_VOUCHER.getEndpointWithId()))
      .then()
      .assertThat()
      .statusCode(404);
  }
}
