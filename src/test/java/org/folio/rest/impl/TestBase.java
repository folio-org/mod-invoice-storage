package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.impl.StorageTestSuite.initSpringContext;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.config.ApplicationConfig;
import org.folio.dbschema.ObjectMapperTool;
import org.folio.rest.jaxrs.model.BatchVoucher;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * When not run from StorageTestSuite then this class invokes StorageTestSuite.before() and
 * StorageTestSuite.after() to allow to run a single test class, for example from within an
 * IDE during development.
 */
public abstract class TestBase {


  private static boolean invokeStorageTestSuiteAfter = false;

  public static final String ISOLATED_TENANT = "isolated";
  public static final String NON_EXISTED_ID = "bad500aa-aaaa-500a-aaaa-aaaaaaaaaaaa";
  public static final Header TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "diku");
  public static final Header TENANT_WITHOUT_DB_HEADER = new Header(OKAPI_HEADER_TENANT, "no_db_tenant");
  public static final Header USER_ID_HEADER = new Header("X-Okapi-User-id", "28d0fb04-d137-11e8-a8d5-f2801f1b9fd1");
  public static final Header X_OKAPI_TOKEN = new Header(OKAPI_HEADER_TOKEN, "eyJhbGciOiJIUzI1NiJ9");
  public static final Header ISOLATED_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, ISOLATED_TENANT);

  public static final String ID = "id";

  private final Map<TestEntities, List<String>> kafkaMessageMethods = Map.of(
    TestEntities.INVOICE, getEnumValuesAsString(InvoiceAuditEvent.Action.class),
    TestEntities.INVOICE_LINES, getEnumValuesAsString(InvoiceLineAuditEvent.Action.class)
  );

  @BeforeAll
  public static void testBaseBeforeClass() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    Vertx vertx = StorageTestSuite.getVertx();
    if (vertx == null) {
      invokeStorageTestSuiteAfter = true;
      StorageTestSuite.before();
    }
    initSpringContext(ApplicationConfig.class);
  }

  @AfterAll
  public static void testBaseAfterClass()  throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
    if (invokeStorageTestSuiteAfter) {
      StorageTestSuite.after();
    }
  }

  ValidatableResponse verifyCollectionQuantity(String endpoint, int quantity, Header tenantHeader) throws MalformedURLException {
    return getData(endpoint, tenantHeader)
      .then()
      .log().all()
      .statusCode(200)
      .body("totalRecords", equalTo(quantity));
  }

  void verifyCollectionQuantity(String endpoint, int quantity) throws MalformedURLException {
    // Verify that there are no existing  records
    verifyCollectionQuantity(endpoint, quantity, TENANT_HEADER);
  }

  @SafeVarargs
  final void givenTestData(Pair<TestEntities, String>... testPairs) throws MalformedURLException {
    for(Pair<TestEntities, String> pair: testPairs) {

      String sample = getFile(pair.getRight());
      String id = new JsonObject(sample).getString("id");
      pair.getLeft().setId(id);

      postData(pair.getLeft().getEndpoint(), sample, ISOLATED_TENANT_HEADER)
        .then()
        .statusCode(201);
    }
  }

  Response getData(String endpoint, Header tenantHeader) throws MalformedURLException {
    return given()
      .header(tenantHeader)
      .contentType(ContentType.JSON)
      .get(storageUrl(endpoint));
  }

  Response postData(String endpoint, String input) throws MalformedURLException {
    return postData(endpoint, input, TENANT_HEADER);
  }

  Response postData(String endpoint, String input, Header tenant) throws MalformedURLException {
    return given()
      .header(tenant)
      .header(USER_ID_HEADER)
      .header(X_OKAPI_TOKEN)
      .accept(ContentType.JSON)
      .contentType(ContentType.JSON)
      .body(input)
      .post(storageUrl(endpoint));
  }

  String createEntity(String endpoint, Object input) throws MalformedURLException {
    return createEntity(endpoint, ObjectMapperTool.valueAsString(input));
  }

  String createEntity(String endpoint, String entity) throws MalformedURLException {
    return postData(endpoint, entity)
      .then().log().ifValidationFails()
        .statusCode(201)
        .extract()
          .path("id");
  }


  void testEntitySuccessfullyFetched(String endpoint, String id) throws MalformedURLException {
    getDataById(endpoint, id)
      .then().log().ifValidationFails()
        .statusCode(200)
        .body("id", equalTo(id));
  }

  Response getDataById(String endpoint, String id) throws MalformedURLException {
    return given()
      .pathParam("id", id)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(endpoint));
  }

  Response getData(String endpoint) throws MalformedURLException {
    return getData(endpoint, TENANT_HEADER);
  }

  void testEntityEdit(String endpoint, String entitySample, String id) throws MalformedURLException {
    putData(endpoint, id, entitySample)
      .then().log().ifValidationFails()
      .statusCode(204);
  }

  void testFetchingUpdatedEntity(String id, TestEntities subObject) throws MalformedURLException {
    Object existedValue = getDataById(subObject.getEndpointWithId(), id)
      .then()
        .statusCode(200).log().ifValidationFails()
        .extract()
          .body()
            .jsonPath()
              .get(subObject.getUpdatedFieldName());
    assertEquals(existedValue, subObject.getUpdatedFieldValue());
  }

  Response putData(String endpoint, String id, String input) throws MalformedURLException {
    return given()
      .pathParam("id", id)
        .header(TENANT_HEADER)
        .contentType(ContentType.JSON)
        .body(input)
          .put(storageUrl(endpoint));
  }

  void deleteDataSuccess(String endpoint, String id) throws MalformedURLException {
    deleteData(endpoint, id)
      .then().log().ifValidationFails()
        .statusCode(204);
  }

  Response deleteData(String endpoint, String id) throws MalformedURLException {
    return deleteData(endpoint, id, TENANT_HEADER);
  }

  Response deleteData(String endpoint, String id, Header tenantHeader) throws MalformedURLException {
    return given()
      .pathParam("id", id)
      .header(tenantHeader)
      .contentType(ContentType.JSON)
      .delete(storageUrl(endpoint));
  }

  void testVerifyEntityDeletion(String endpoint, String id) throws MalformedURLException {
    getDataById(endpoint, id)
      .then()
        .statusCode(404);
  }

  void testInvalidCQLQuery(String endpoint) throws MalformedURLException {
    getData(endpoint).then().log().ifValidationFails()
      .statusCode(400);
  }

  Response getDataByParam(String endpoint, Map<String, Object> params) throws MalformedURLException {
    return given()
      .params(params)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
        .get(storageUrl(endpoint));
  }

  void testAllFieldsExists(JsonObject extracted, JsonObject sampleObject) {
    sampleObject.remove("id");
    Set<String> fieldsNames = sampleObject.fieldNames();
    for (String fieldName : fieldsNames) {
      Object sampleField = sampleObject.getValue(fieldName);
      if (sampleField instanceof JsonObject) {
        testAllFieldsExists((JsonObject) sampleField, (JsonObject) extracted.getValue(fieldName));
      } else {
        assertEquals(sampleObject.getValue(fieldName).toString(), extracted.getValue(fieldName).toString());
      }
    }
  }

  static String getFile(String filename) {
    String value = "";
    try (InputStream inputStream = TestBase.class.getClassLoader().getResourceAsStream(filename)) {
      if (inputStream != null) {
        value = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      value = "";
    }
    return value;
  }

  RequestSpecification commonRequestSpec(){
    return new RequestSpecBuilder().
      addHeader(TENANT_HEADER.getName(), TENANT_HEADER.getValue()).
      addHeader(USER_ID_HEADER.getName(), USER_ID_HEADER.getValue()).
      addHeader(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue()).
      setContentType(ContentType.JSON).
      build();
  }

  RequestSpecification isolatedRequestSpec(){
    return new RequestSpecBuilder().
      addHeader(ISOLATED_TENANT_HEADER.getName(), ISOLATED_TENANT_HEADER.getValue()).
      addHeader(USER_ID_HEADER.getName(), USER_ID_HEADER.getValue()).
      addHeader(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue()).
      setContentType(ContentType.JSON).
      build();
  }

  void assertAllFieldsExistAndEqual(JsonObject sample, Response response) {
     JsonObject sampleJson = JsonObject.mapFrom(sample.mapTo(BatchVoucher.class));
     JsonObject responseJson = JsonObject.mapFrom(response.then().extract().as(BatchVoucher.class));
     testAllFieldsExists(responseJson, sampleJson);
  }

  void verifyKafkaMessagesSentIfNeeded(String eventType, TestEntities testEntity, String tenant, String userId, int expected) {
    if (kafkaMessageMethods.containsKey(testEntity) && kafkaMessageMethods.get(testEntity).contains(eventType)) {
      List<String> events = StorageTestSuite.checkKafkaEventSent(tenant, eventType, expected, userId);
      assertEquals(expected, events.size());
      for (String event : events) {
        assertEquals(event, eventType);
      }
    }
  }

  private static <T extends Enum<T>> List<String> getEnumValuesAsString(Class<T> enumClass) {
    return Arrays.stream(enumClass.getEnumConstants()).map(Enum::toString).toList();
  }

}
