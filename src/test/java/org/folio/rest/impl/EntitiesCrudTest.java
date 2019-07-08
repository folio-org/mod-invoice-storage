package org.folio.rest.impl;

import java.net.MalformedURLException;
import java.util.stream.Stream;

import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EntitiesCrudTest extends TestBase {

  private final Logger logger = LoggerFactory.getLogger(EntitiesCrudTest.class);
  private String sample = null;

  static Stream<TestEntities> deleteOrder() {
    return Stream.of(
      TestEntities.ACQUISITIONS_UNIT_ASSIGNMENTS,
      TestEntities.VOUCHER_LINES,
      TestEntities.VOUCHER,
      TestEntities.INVOICE_LINES,
      TestEntities.INVOICE);
  }

  static Stream<TestEntities> deleteFailOrder() {
    return Stream.of(
      TestEntities.VOUCHER,
      TestEntities.INVOICE);
  }

  static Stream<TestEntities> createFailOrder() {
    return Stream.of(
      TestEntities.ACQUISITIONS_UNIT_ASSIGNMENTS,
      TestEntities.VOUCHER_LINES,
      TestEntities.VOUCHER,
      TestEntities.INVOICE_LINES);
  }

  @ParameterizedTest
  @Order(1)
  @EnumSource(TestEntities.class)
  void testVerifyCollection(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Verifying database's initial state ... ", testEntity.name()));
    verifyCollectionQuantity(testEntity.getEndpoint(), 0);

  }

  @ParameterizedTest
  @Order(2)
  @EnumSource(TestEntities.class)
  void testPostData(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Creating %s ... ", testEntity.name(), testEntity.name()));
    sample = getSample(testEntity.getSampleFileName());
    Response response = postData(testEntity.getEndpoint(), sample);
    testEntity.setId(response.then()
      .extract()
      .path("id"));
    logger.info(String.format("--- mod-invoice-storage %s test: Valid fields exists ... ", testEntity.name()));
    JsonObject sampleJson = convertToMatchingModelJson(sample, testEntity);
    JsonObject responseJson = JsonObject.mapFrom(response.then()
      .extract()
      .response()
      .as(testEntity.getClazz()));
    testAllFieldsExists(responseJson, sampleJson);
  }

  @ParameterizedTest
  @Order(3)
  @EnumSource(TestEntities.class)
  void testVerifyCollectionQuantity(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Verifying only 1 adjustment was created ... ", testEntity.name()));
    verifyCollectionQuantity(testEntity.getEndpoint(), 1);

  }

  @ParameterizedTest
  @Order(4)
  @EnumSource(TestEntities.class)
  void testGetById(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Fetching %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    testEntitySuccessfullyFetched(testEntity.getEndpointWithId(), testEntity.getId());
  }

  @ParameterizedTest
  @Order(5)
  @EnumSource(TestEntities.class)
  void testPutById(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Editing %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    JsonObject catJSON = new JsonObject(getSample(testEntity.getSampleFileName()));
    catJSON.put("id", testEntity.getId());
    catJSON.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
    testEntityEdit(testEntity.getEndpointWithId(), catJSON.toString(), testEntity.getId());

  }

  @ParameterizedTest
  @Order(6)
  @EnumSource(TestEntities.class)
  void testVerifyPut(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Fetching updated %s with ID: %s", testEntity.name(),
        testEntity.name(), testEntity.getId()));
    testFetchingUpdatedEntity(testEntity.getId(), testEntity);
  }

  @ParameterizedTest
  @Order(7)
  @MethodSource("deleteFailOrder")
  void testDeleteEndpointForeignKeyFailure(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storages %s test: Deleting %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    deleteData(testEntity.getEndpointWithId(), testEntity.getId()).then()
      .log()
      .ifValidationFails()
      .statusCode(400);
  }

  @ParameterizedTest
  @Order(8)
  @MethodSource("deleteOrder")
  void testDeleteEndpoint(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storages %s test: Deleting %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    deleteData(testEntity.getEndpointWithId(), testEntity.getId()).then()
      .log()
      .ifValidationFails()
      .statusCode(204);
  }

  @ParameterizedTest
  @Order(9)
  @EnumSource(TestEntities.class)
  void testVerifyDelete(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storages %s test: Verify %s is deleted with ID: %s", testEntity.name(),
        testEntity.name(), testEntity.getId()));
    testVerifyEntityDeletion(testEntity.getEndpointWithId(), testEntity.getId());
  }

  @ParameterizedTest
  @MethodSource("createFailOrder")
  void testPostFailsOnForeignKeyDependencies(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Creating %s ... fails", testEntity.name(), testEntity.name()));
    sample = getSample(testEntity.getSampleFileName());
    Response response = postData(testEntity.getEndpoint(), sample);
    response.then()
      .statusCode(400);

  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testFetchEntityWithNonExistedId(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s get by id test: Invalid %s: %s", testEntity.name(), testEntity.name(),
        NON_EXISTED_ID));
    getDataById(testEntity.getEndpointWithId(), NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testEditEntityWithNonExistedId(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s put by id test: Invalid %s: %s", testEntity.name(), testEntity.name(),
        NON_EXISTED_ID));
    String sampleData = getFile(testEntity.getSampleFileName());
    putData(testEntity.getEndpointWithId(), NON_EXISTED_ID, sampleData).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testDeleteEntityWithNonExistedId(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s delete by id test: Invalid %s: %s", testEntity.name(), testEntity.name(),
        NON_EXISTED_ID));
    deleteData(testEntity.getEndpointWithId(), NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testGetEntitiesWithInvalidCQLQuery(TestEntities testEntity) throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage %s test: Invalid CQL query", testEntity.name()));
    testInvalidCQLQuery(testEntity.getEndpoint() + "?query=invalid-query");
  }

  private String getSample(String fileName) {
    return getFile(fileName);
  }

  private JsonObject convertToMatchingModelJson(String sample, TestEntities testEntity) {
    return JsonObject.mapFrom(new JsonObject(sample).mapTo(testEntity.getClazz()));
  }

}
