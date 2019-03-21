package org.folio.rest.impl;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.folio.rest.impl.utils.TestEntities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.MalformedURLException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class CrudTest extends TestBase {

	private final Logger logger = LoggerFactory.getLogger(CrudTest.class);
	
  @Parameterized.Parameter public TestEntities testEntity;

  @Parameterized.Parameters(name = "{index}:{0}")
  public static TestEntities[] data() {
    return TestEntities.values();
  }

  @Test
  public void testPositiveCases() throws MalformedURLException {
    String sampleId = null;
    try {

      logger.info(String.format("--- mod-invoice-storage %s test: Verifying database's initial state ... ", testEntity.name()));
      verifyCollectionQuantity(testEntity.getEndpoint(), 0);

      logger.info(String.format("--- mod-invoice-storage %s test: Creating %s ... ", testEntity.name(), testEntity.name()));
      String sample = getFile(testEntity.getSampleFileName());
      logger.info("--- mod-invoice-storage --- sample file: " + sample );
      logger.info("--- mod-invoice-storage --- testEntity.getEndpoint(): " + testEntity.getEndpoint() );
      Response response = postData(testEntity.getEndpoint(), sample);
      logger.info("--- mod-invoice-storage --- response: " + response.getStatusCode() );
      sampleId = response.then().extract().path("id");
      logger.info("--- mod-invoice-storage --- sampleID: " + sampleId );
    } catch (Exception e) {
      logger.error(String.format("--- mod-invoice-storage-test: %s API ERROR: %s", testEntity.name(), e.getMessage()));
      fail(e.getMessage());
    } finally {

    }

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
