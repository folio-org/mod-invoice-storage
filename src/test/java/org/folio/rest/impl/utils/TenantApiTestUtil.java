package org.folio.rest.impl.utils;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.MalformedURLException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.URL_TO_HEADER;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;

public class TenantApiTestUtil {

  public static final String TENANT_ENDPOINT = "/_/tenant";
  private static final Header USER_ID_HEADER = new Header("X-Okapi-User-id", "28d0fb04-d137-11e8-a8d5-f2801f1b9fd1");

  private TenantApiTestUtil() {

  }

  public static JsonObject prepareTenantBody(boolean isLoadSampleData, boolean isUpgrade) {
    JsonArray parameterArray = new JsonArray();
    parameterArray.add(new JsonObject().put("key", "loadSample").put("value", isLoadSampleData));
    JsonObject jsonBody = new JsonObject();
    jsonBody.put("module_to", "mod-invoice-storage-1.0.0");
    jsonBody.put("parameters", parameterArray);
    if(isUpgrade)
      jsonBody.put("module_from", "mod-invoice-storage-1.0.0");
    return jsonBody;
  }

  public static void prepareTenant(Header tenantHeader, boolean isLoadSampleData) throws MalformedURLException {
    JsonObject jsonBody = prepareTenantBody(isLoadSampleData, false);
    postToTenant(tenantHeader, jsonBody).statusCode(201);
  }

  public static ValidatableResponse postToTenant(Header tenantHeader, JsonObject jsonBody) throws MalformedURLException {
    return given()
      .header(tenantHeader)
      .header(URL_TO_HEADER)
      .header(USER_ID_HEADER)
      .contentType(ContentType.JSON)
      .body(jsonBody.encodePrettily())
      .post(storageUrl(TENANT_ENDPOINT))
        .then();
  }

  public static void deleteTenant(Header tenantHeader)
    throws MalformedURLException {
    given()
      .header(tenantHeader)
      .contentType(ContentType.JSON)
      .delete(storageUrl(TENANT_ENDPOINT))
        .then()
          .statusCode(204);
  }
}
