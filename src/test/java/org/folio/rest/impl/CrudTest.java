package org.folio.rest.impl;

import io.restassured.http.ContentType;
import org.folio.rest.impl.utils.TestEntities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.MalformedURLException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;

@RunWith(Parameterized.class)
public class CrudTest extends TestBase {

  @Parameterized.Parameter public TestEntities testEntity;

  @Parameterized.Parameters(name = "{index}:{0}")
  public static TestEntities[] data() {
    return TestEntities.values();
  }

  @Test
  public void getInvoicesTest() throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(testEntity.getEndpoint()))
        .then()
          .statusCode(501);
  }

  @Test
  public void getByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(testEntity.getEndpointWithId()))
        .then()
          .statusCode(501);
  }

  @Test
  public void putByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .body(getFile(testEntity.getSampleFileName()))
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .put(storageUrl(testEntity.getEndpointWithId()))
        .then()
          .statusCode(501);
  }

  @Test
  public void deleteByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .delete(storageUrl(testEntity.getEndpointWithId()))
        .then()
          .statusCode(501);
  }

  @Test
  public void postTest() throws MalformedURLException {
    given()
      .body(getFile(testEntity.getSampleFileName()))
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .post(storageUrl(testEntity.getEndpoint()))
        .then()
          .statusCode(501);
  }

}
