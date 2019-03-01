package org.folio.rest.imp;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.restassured.RestAssured.given;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

public class InvoicesTest {
  private static final Logger logger = LoggerFactory.getLogger(InvoicesTest.class);
  private static final String INVOICE_STORAGE_INVOICES_PATH = "/invoice-storage/invoices";
  private static final String INVOICE_STORAGE_INVOICES_LINES_PATH = "/invoice-storage/invoice-lines";
  private static final String INVOICE_STORAGE_INVOICE_ID_PATH = INVOICE_STORAGE_INVOICES_PATH + "/{id}";
  private static final String INVOICE_STORAGE_INVOICE_LINE_ID_PATH = INVOICE_STORAGE_INVOICES_LINES_PATH + "/{id}";
  private static final String TENANT_ENDPOINT = "/_/tenant";
  private static final String NON_EXISTED_ID = "bad500aa-aaaa-500a-aaaa-aaaaaaaaaaaa";

  private static Vertx vertx;
  private static int port = NetworkUtils.nextFreePort();
  private static final Header URL_TO_HEADER = new Header("X-Okapi-Url-to","http://localhost:"+port);
  private static final Header USER_ID_HEADER = new Header("X-Okapi-User-id", "28d0fb04-d137-11e8-a8d5-f2801f1b9fd1");
  private static final String TENANT_NAME = "diku";
  private static final Header TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, TENANT_NAME);

  @BeforeClass
  public static void before() throws InterruptedException, ExecutionException, TimeoutException, IOException {

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

    undeploymentComplete.get(20, TimeUnit.SECONDS);
    logger.info("Stop database");
    PostgresClient.stopEmbeddedPostgres();
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

  private static void deleteTenant()
    throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .delete(storageUrl(TENANT_ENDPOINT))
        .then()
          .statusCode(204);
  }

  private static URL storageUrl(String path) throws MalformedURLException {
    return new URL("http", "localhost", port, path);
  }

  @Test
  public void getInvoicesTest() throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(INVOICE_STORAGE_INVOICES_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void getInvoicesByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(INVOICE_STORAGE_INVOICE_ID_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void putInvoicesByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .body(getFile("invoice.sample"))
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .put(storageUrl(INVOICE_STORAGE_INVOICE_ID_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void deleteInvoicesByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .delete(storageUrl(INVOICE_STORAGE_INVOICE_ID_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void postInvoicesTest() throws MalformedURLException {
    given()
      .body(getFile("invoice.sample"))
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .post(storageUrl(INVOICE_STORAGE_INVOICES_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void getInvoiceLinesTest() throws MalformedURLException {
    given()
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(INVOICE_STORAGE_INVOICES_LINES_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void getInvoiceLinesByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .get(storageUrl(INVOICE_STORAGE_INVOICE_LINE_ID_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void putInvoiceLinesByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .body(getFile("invoice_line.sample"))
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .put(storageUrl(INVOICE_STORAGE_INVOICE_LINE_ID_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void deleteInvoiceLinesByIdTest() throws MalformedURLException {
    given()
      .pathParam("id", NON_EXISTED_ID)
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .delete(storageUrl(INVOICE_STORAGE_INVOICE_LINE_ID_PATH))
        .then()
          .statusCode(500);
  }

  @Test
  public void postInvoiceLinesTest() throws MalformedURLException {
    given()
      .body(getFile("invoice_line.sample"))
      .header(TENANT_HEADER)
      .contentType(ContentType.JSON)
      .post(storageUrl(INVOICE_STORAGE_INVOICES_LINES_PATH))
        .then()
          .statusCode(500);
  }

  private String getFile(String filename) {
    String value = "";
    try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filename)) {
      if (inputStream != null) {
        value = IOUtils.toString(inputStream, "UTF-8");
      }
    } catch (Exception e) {
      value = "";
    }
    return value;
  }

}
