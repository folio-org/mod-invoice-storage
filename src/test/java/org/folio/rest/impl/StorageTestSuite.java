package org.folio.rest.impl;

import static org.folio.rest.impl.TestBase.TENANT_HEADER;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.tools.utils.Envs;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.runner.RunWith;

import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.testcontainers.containers.PostgreSQLContainer;

public class StorageTestSuite {
  private static final Logger logger = LogManager.getLogger(StorageTestSuite.class);

  private static Vertx vertx;
  private static final int port = NetworkUtils.nextFreePort();
  public static final Header URL_TO_HEADER = new Header("X-Okapi-Url-to", "http://localhost:" + port);
  private static TenantJob tenantJob;
  public static final String POSTGRES_DOCKER_IMAGE = "postgres:12-alpine";
  private static PostgreSQLContainer<?> postgresSQLContainer;

  private StorageTestSuite() {}

  public static URL storageUrl(String path) throws MalformedURLException {
    return new URL("http", "localhost", port, path);
  }

  public static Vertx getVertx() {
    return vertx;
  }

  @BeforeAll
  public static void before() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    // tests expect English error messages only, no Danish/German/...
    Locale.setDefault(Locale.US);

    vertx = Vertx.vertx();

    logger.info("Start container database");

    // databaseName = "test", username = "test" password = "test", port = random
    postgresSQLContainer = new PostgreSQLContainer<>(POSTGRES_DOCKER_IMAGE);

    postgresSQLContainer.start();

    Envs.setEnv(
      postgresSQLContainer.getHost(),
      postgresSQLContainer.getFirstMappedPort(),
      postgresSQLContainer.getUsername(),
      postgresSQLContainer.getPassword(),
      postgresSQLContainer.getDatabaseName()
    );;

    DeploymentOptions options = new DeploymentOptions();

    options.setConfig(new JsonObject().put("http.port", port).put(HttpClientMock2.MOCK_MODE, "true"));
    options.setWorker(true);

    startVerticle(options);

    tenantJob = prepareTenant(TENANT_HEADER, false, false);
  }

  @AfterAll
  public static void after() throws InterruptedException, ExecutionException, TimeoutException {
    logger.info("Delete tenant");
    deleteTenant(tenantJob, TENANT_HEADER);

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
    PostgresClient.stopPostgresTester();
    postgresSQLContainer.stop();
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


  @Nested
  class InvoiceTestNested extends InvoiceTest {}
  @Nested
  class InvoiceNumberTestNested extends InvoiceNumberTest {}
  @Nested
  class InvoiceLineNumberTestNested extends InvoiceLineNumberTest {}
  @Nested
  class TenantSampleDataTestNested extends TenantSampleDataTest {}
  @Nested
  class EntitiesCrudTestNested extends EntitiesCrudTest {}
  @Nested
  class SystemDataLoadingTestNested extends SystemDataLoadingTest {}
  @Nested
  class DocumentTestNested extends DocumentTest {}
  @Nested
  class HelperUtilsTestNested extends HelperUtilsTest{}
  @Nested
  class ExportConfigCredentialsTestNested extends ExportConfigCredentialsTest {}
  @Nested
  class BatchVoucherTestNested extends BatchVoucherTest{}
  @Nested
  class BatchVoucherExportsTestNested extends BatchVoucherExportsImplTest{}
  @Nested
  class VoucherNumberTestNested extends VoucherNumberTest {}
}
