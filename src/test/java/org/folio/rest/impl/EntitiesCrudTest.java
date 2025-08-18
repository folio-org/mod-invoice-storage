package org.folio.rest.impl;

import java.net.MalformedURLException;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EntitiesCrudTest extends TestBase {

  private static final Logger log = LogManager.getLogger(EntitiesCrudTest.class);

  private static final String CREATE_EVENT = "CREATE";
  private static final String UPDATE_EVENT = "EDIT";

  private String sample = null;

  static Stream<TestEntities> deleteOrder() {
    return Stream.of(
      TestEntities.VOUCHER_LINES,
      TestEntities.VOUCHER,
      TestEntities.INVOICE_LINES,
      TestEntities.INVOICE,
      TestEntities.BATCH_VOUCHER_EXPORT_CONFIGS,
      TestEntities.BATCH_VOUCHER_EXPORTS,
      TestEntities.BATCH_GROUP,
      TestEntities.SETTING);
  }

  static Stream<TestEntities> deleteFailOrder() {
    return Stream.of(
      TestEntities.VOUCHER,
      TestEntities.INVOICE);
  }

  static Stream<TestEntities> createFailOrder() {
    return Stream.of(
      TestEntities.VOUCHER_LINES,
      TestEntities.VOUCHER,
      TestEntities.INVOICE_LINES);
  }

  @ParameterizedTest
  @Order(1)
  @EnumSource(value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE)
  void testVerifyCollection(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Verifying database's initial state ... ", testEntity.name()));
    verifyCollectionQuantity(testEntity.getEndpoint(), TestEntities.BATCH_GROUP.equals(testEntity)? 1 : 0);
  }

  @ParameterizedTest
  @Order(2)
  @EnumSource(TestEntities.class)
  void testPostData(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Creating %s ... ", testEntity.name(), testEntity.name()));
    sample = getSample(testEntity.getSampleFileName());
    Response response = postData(testEntity.getEndpoint(), sample);
    testEntity.setId(response.then()
      .extract()
      .path("id"));
    log.info(String.format("--- mod-invoice-storage %s test: Valid fields exists ... ", testEntity.name()));
    JsonObject sampleJson = convertToMatchingModelJson(sample, testEntity);
    JsonObject responseJson = JsonObject.mapFrom(response.then()
      .extract()
      .response()
      .as(testEntity.getClazz()));
    testAllFieldsExists(responseJson, sampleJson);
  }

  @ParameterizedTest
  @Order(3)
  @EnumSource(value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE)
  void testVerifyCollectionQuantity(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Verifying only 1 adjustment was created ... ", testEntity.name()));
    verifyCollectionQuantity(testEntity.getEndpoint(), TestEntities.BATCH_GROUP.equals(testEntity)? 2 : 1);
    verifyKafkaMessagesSentIfNeeded(CREATE_EVENT, testEntity, TENANT_HEADER.getValue(), 1);
  }

  @ParameterizedTest
  @Order(4)
  @EnumSource(TestEntities.class)
  void testGetById(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Fetching %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    testEntitySuccessfullyFetched(testEntity.getEndpointWithId(), testEntity.getId());
  }

  @ParameterizedTest
  @Order(5)
  @EnumSource(value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE)
  void testPutById(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Editing %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    JsonObject catJSON = new JsonObject(getSample(testEntity.getSampleFileName()));
    catJSON.put("id", testEntity.getId());
    catJSON.put(testEntity.getUpdatedFieldName(), testEntity.getUpdatedFieldValue());
    testEntityEdit(testEntity.getEndpointWithId(), catJSON.toString(), testEntity.getId());

  }

  @ParameterizedTest
  @Order(6)
  @EnumSource(value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE)
  void testVerifyPut(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Fetching updated %s with ID: %s", testEntity.name(),
        testEntity.name(), testEntity.getId()));
    testFetchingUpdatedEntity(testEntity.getId(), testEntity);
    verifyKafkaMessagesSentIfNeeded(UPDATE_EVENT, testEntity, TENANT_HEADER.getValue(), 1);
  }

  @ParameterizedTest
  @Order(7)
  @MethodSource("deleteFailOrder")
  void testDeleteEndpointForeignKeyFailure(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storages %s test: Deleting %s with ID: %s", testEntity.name(), testEntity.name(),
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
    log.info(String.format("--- mod-invoice-storages %s test: Deleting %s with ID: %s", testEntity.name(), testEntity.name(),
        testEntity.getId()));
    deleteData(testEntity.getEndpointWithId(), testEntity.getId()).then()
      .log()
      .ifValidationFails()
      .statusCode(204);
  }

  @ParameterizedTest
  @Order(9)
  @EnumSource(value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE)
  void testVerifyDelete(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storages %s test: Verify %s is deleted with ID: %s", testEntity.name(),
        testEntity.name(), testEntity.getId()));
    testVerifyEntityDeletion(testEntity.getEndpointWithId(), testEntity.getId());
  }

  @ParameterizedTest
  @MethodSource("createFailOrder")
  void testPostFailsOnForeignKeyDependencies(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Creating %s ... fails", testEntity.name(), testEntity.name()));
    sample = getSample(testEntity.getSampleFileName());
    Response response = postData(testEntity.getEndpoint(), sample);
    response.then()
      .statusCode(400);

  }

  @ParameterizedTest
  @EnumSource(TestEntities.class)
  void testFetchEntityWithNonExistedId(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s get by id test: Invalid %s: %s", testEntity.name(), testEntity.name(),
        NON_EXISTED_ID));
    getDataById(testEntity.getEndpointWithId(), NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @ParameterizedTest
  @EnumSource(
    value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE
  )
  void testEditEntityWithNonExistedId(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s put by id test: Invalid %s: %s", testEntity.name(), testEntity.name(),
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
    log.info(String.format("--- mod-invoice-storage %s delete by id test: Invalid %s: %s", testEntity.name(), testEntity.name(),
        NON_EXISTED_ID));
    deleteData(testEntity.getEndpointWithId(), NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @ParameterizedTest
  @EnumSource(
    value = TestEntities.class,
    names = {"BATCH_VOUCHER"},
    mode = EnumSource.Mode.EXCLUDE
  )
  void testGetEntitiesWithInvalidCQLQuery(TestEntities testEntity) throws MalformedURLException {
    log.info(String.format("--- mod-invoice-storage %s test: Invalid CQL query", testEntity.name()));
    testInvalidCQLQuery(testEntity.getEndpoint() + "?query=invalid-query");
  }

  private String getSample(String fileName) {
    return getFile(fileName);
  }

  private JsonObject convertToMatchingModelJson(String sample, TestEntities testEntity) {
    return JsonObject.mapFrom(new JsonObject(sample).mapTo(testEntity.getClazz()));
  }

}
