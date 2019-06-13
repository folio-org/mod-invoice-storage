package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.folio.rest.utils.TestEntities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@RunWith(Parameterized.class)
public class CrudTest extends TestBase {

private final Logger logger = LoggerFactory.getLogger(CrudTest.class);

  @Parameterized.Parameter public TestEntities testEntity;

  @Parameterized.Parameters(name = "{index}:{0}")
  public static TestEntities[] data() {
    return TestEntities.values();
  }

  private static ValidatorFactory validationFactory = Validation.buildDefaultValidatorFactory();

  @Test
  public void testPositiveCases() throws MalformedURLException {
  	String sampleId = null;
    try {
      logger.info(String.format("--- mod-invoice-storage %s test: Verifying database's initial state ... ", testEntity.name()));
      int initialQuantity = verifyGetCollection(testEntity.getEndpoint());

      logger.info(String.format("--- mod-invoice-storage %s test: Creating %s ... ", testEntity.name(), testEntity.name()));
      String sample = getFile(testEntity.getSampleFileName());
      JsonObject jsonSample = new JsonObject(sample);
      jsonSample.remove("id");
      Response response = postData(testEntity.getEndpoint(), jsonSample.encodePrettily());
      sampleId = response.then().log().ifValidationFails().extract().path(ID);
      
      logger.info(String.format("--- mod-invoice-storage %s test: Valid fields exists ... ", testEntity.name()));
      JsonObject sampleJson = convertToMatchingModelJson(sample, testEntity);
      JsonObject responseJson = JsonObject.mapFrom(response.then().extract().response().as(testEntity.getClazz()));
      testAllFieldsExists(responseJson, sampleJson);
      
      logger.info(String.format("--- mod-invoice-storage %s test: Verifying only 1 invoice was created ... ", testEntity.name()));
      verifyCollectionQuantity(testEntity.getEndpoint(),initialQuantity + 1);
      
      logger.info(String.format("--- mod-invoice-storage %s test: Fetching %s with ID: %s", testEntity.name(), testEntity.name(), sampleId));
      testEntitySuccessfullyFetched(testEntity.getEndpointWithId(), sampleId);
      
      logger.info(String.format("--- mod-invoice-storage %s test: Editing %s with ID: %s", testEntity.name(), testEntity.name(), sampleId));
      JsonObject catJSON = new JsonObject(sample);
      catJSON.put("id", sampleId);
      catJSON.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
      testEntityEdit(testEntity.getEndpointWithId(), catJSON.toString(), sampleId);
      
      logger.info(String.format("--- mod-invoice-storage %s test: Fetching updated %s with ID: %s", testEntity.name(), testEntity.name(), sampleId));
      testFetchingUpdatedEntity(sampleId, testEntity);    
      
		} catch (Exception e) {
			  logger.error(String.format("--- mod-invoice-storage-test: %s API ERROR: ", testEntity.name()), e);
			  fail(e.getMessage());
		} finally {
			  logger.info(String.format("--- mod-invoice-storage %s test: Deleting %s with ID: %s", testEntity.name(), testEntity.name(), sampleId));
			  deleteDataSuccess(testEntity.getEndpointWithId(), sampleId);

			  logger.info(String.format("--- mod-invoice-storage %s test: Verify %s is deleted with ID: %s", testEntity.name(), testEntity.name(), sampleId));
			  testVerifyEntityDeletion(testEntity.getEndpointWithId(), sampleId);
		}
  }

  private JsonObject convertToMatchingModelJson(String sample, TestEntities testEntity) {
    // Convert to corresponding class to check that only expected properties are defined
    Object obj = new JsonObject(sample).mapTo(testEntity.getClazz());
    JsonObject content = JsonObject.mapFrom(obj);
    // Perform validation to remove read-only properties
    Set<? extends ConstraintViolation<?>> validationErrors = validationFactory.getValidator().validate(obj);
    for (ConstraintViolation<?> cv : validationErrors) {
      // read only fields are marked with annotation @Null
      if (cv.getConstraintDescriptor().getAnnotation() instanceof javax.validation.constraints.Null) {
        content.remove(cv.getPropertyPath().toString());
      }
    }
    return content;
  }

  @Test
  public void testFetchEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s get by id test: Invalid %s: %s", testEntity.name(), testEntity.name(), NON_EXISTED_ID));
    getDataById(testEntity.getEndpointWithId(), NON_EXISTED_ID).then().log().ifValidationFails()
      .statusCode(404);
  }
  
  @Test
  public void testEditEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s put by id test: Invalid %s: %s", testEntity.name(), testEntity.name(), NON_EXISTED_ID));
    String sampleData = getFile(testEntity.getSampleFileName());
    putData(testEntity.getEndpointWithId(), NON_EXISTED_ID, sampleData)
      .then().log().ifValidationFails()
        .statusCode(404);
  }

  @Test
  public void deleteByNonExistedIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .delete(storageUrl(testEntity.getEndpointWithId()))
        .then()
          .statusCode(404);
  }
  
  @Test
  public void testDeleteEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s delete by id test: Invalid %s: %s", testEntity.name(), testEntity.name(), NON_EXISTED_ID));
    deleteData(testEntity.getEndpointWithId(), NON_EXISTED_ID)
      .then().log().ifValidationFails()
        .statusCode(404);
  }

  @Test
  public void testGetEntitiesWithInvalidCQLQuery() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Invalid CQL query", testEntity.name()));
    testInvalidCQLQuery(testEntity.getEndpoint() + "?query=invalid-query");
  }
}
