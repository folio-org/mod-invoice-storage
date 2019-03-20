package org.folio.rest.impl;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.TestBase.TENANT_HEADER;


@RunWith(Suite.class)

@Suite.SuiteClasses({
  CrudTest.class
})

public class StorageTestSuite {
  private static final Logger logger = LoggerFactory.getLogger(StorageTestSuite.class);

  private static Vertx vertx;
  private static int port = NetworkUtils.nextFreePort();
  public static final Header URL_TO_HEADER = new Header("X-Okapi-Url-to","http://localhost:"+port);
  private static final Header USER_ID_HEADER = new Header("X-Okapi-User-id", "28d0fb04-d137-11e8-a8d5-f2801f1b9fd1");
  private static final String TENANT_ENDPOINT = "/_/tenant";

  private StorageTestSuite() {}

  public static URL storageUrl(String path) throws MalformedURLException {
    return new URL("http", "localhost", port, path);
  }

  public static Vertx getVertx() {
    return vertx;
  }

  @BeforeClass
  public static void before() throws IOException, InterruptedException, ExecutionException, TimeoutException {

    // tests expect English error messages only, no Danish/German/...
    Locale.setDefault(Locale.US);


    vertx = Vertx.vertx();

    logger.info("Start embedded database");
    PostgresClient.setIsEmbedded(true);
    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    DeploymentOptions options = new DeploymentOptions();

    options.setConfig(new JsonObject().put("http.port", port).put(HttpClientMock2.MOCK_MODE, "true"));
    options.setWorker(true);

    startVerticle(options);

    prepareTenant();
  }

  @AfterClass
  public static void after() throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException {
    logger.info("Delete tenant");
    deleteTenant();

    CompletableFuture<String> undeploymentComplete = new CompletableFuture<>();

    vertx.close(res -> {
      if(res.succeeded()) {
        undeploymentComplete.complete(null);
      }
      else {
        undeploymentComplete.completeExceptionally(res.cause());
      }
    });

    undeploymentComplete.get(60, TimeUnit.SECONDS);
    logger.info("Stop database");
    PostgresClient.stopEmbeddedPostgres();
  }

  private static void startVerticle(DeploymentOptions options)
    throws InterruptedException, ExecutionException, TimeoutException {

    logger.info("Start verticle");

    CompletableFuture<String> deploymentComplete = new CompletableFuture<>();

    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      if(res.succeeded()) {
        deploymentComplete.complete(res.result());
      }
      else {
        deploymentComplete.completeExceptionally(res.cause());
      }
    });

    deploymentComplete.get(60, TimeUnit.SECONDS);
  }

  private static void prepareTenant() throws MalformedURLException {
    JsonObject jsonBody = prepareTenantBody();
    postToTenant(jsonBody).statusCode(201);
  }

  private static JsonObject prepareTenantBody() {
    JsonObject jsonBody = new JsonObject();
    jsonBody.put("module_to", "mod-orders-storage-1.0.0");
    return jsonBody;
  }

  private static ValidatableResponse postToTenant(JsonObject jsonBody) throws MalformedURLException {
    return given()
      .header(TENANT_HEADER)
      .header(URL_TO_HEADER)
      .header(USER_ID_HEADER)
      .contentType(ContentType.JSON)
      .body(jsonBody.encodePrettily())
        .post(storageUrl(TENANT_ENDPOINT))
          .then();
  }

  private static void deleteTenant()
    throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .delete(storageUrl(TENANT_ENDPOINT))
        .then()
          .statusCode(204);
  }


}
