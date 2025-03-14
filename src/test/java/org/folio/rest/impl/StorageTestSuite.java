package org.folio.rest.impl;

import static net.mguenther.kafka.junit.EmbeddedKafkaClusterConfig.defaultClusterConfig;
import static org.folio.kafka.KafkaTopicNameHelper.getDefaultNameSpace;
import static org.folio.rest.impl.TestBase.TENANT_HEADER;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.HelperUtilsTest;
import org.folio.service.order.OrderStorageServiceTest;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import io.restassured.http.Header;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import net.mguenther.kafka.junit.EmbeddedKafkaCluster;
import net.mguenther.kafka.junit.ObserveKeyValues;

public class StorageTestSuite {
  private static final Logger log = LogManager.getLogger(StorageTestSuite.class);

  private static Vertx vertx;
  private static final int port = NetworkUtils.nextFreePort();
  public static final Header URL_TO_HEADER = new Header("X-Okapi-Url-to", "http://localhost:" + port);
  private static TenantJob tenantJob;

  public static EmbeddedKafkaCluster KAFKA_CLUSTER;
  public static final String KAFKA_ENV_VALUE = "test-env";
  private static final String KAFKA_HOST = "KAFKA_HOST";
  private static final String KAFKA_PORT = "KAFKA_PORT";
  private static final String KAFKA_ENV = "ENV";
  private static final String OKAPI_URL_KEY = "OKAPI_URL";
  public static final int MOCK_KAFKA_PORT = NetworkUtils.nextFreePort();


  private StorageTestSuite() {}

  public static URL storageUrl(String path) throws MalformedURLException {
    return new URL("http", "localhost", port, path);
  }

  public static Vertx getVertx() {
    return vertx;
  }

  public static void autowireDependencies(Object target) {
    SpringContextUtil.autowireDependenciesFromFirstContext(target, getVertx());
  }

  public static void initSpringContext(Class<?> defaultConfiguration) {
    SpringContextUtil.init(vertx, getFirstContextFromVertx(vertx), defaultConfiguration);
  }

  private static Context getFirstContextFromVertx(Vertx vertx) {
    return vertx.deploymentIDs().stream().flatMap((id) -> ((VertxImpl)vertx)
        .getDeployment(id).getVerticles().stream())
      .map(StorageTestSuite::getContextWithReflection)
      .filter(Objects::nonNull)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Spring context was not created"));
  }

  private static Context getContextWithReflection(Verticle verticle) {
    try {
      Field field = AbstractVerticle.class.getDeclaredField("context");
      field.setAccessible(true);
      return ((Context)field.get(verticle));
    } catch (NoSuchFieldException | IllegalAccessException var2) {
      return null;
    }
  }

  @BeforeAll
  public static void before() throws InterruptedException, ExecutionException, TimeoutException {
    // tests expect English error messages only, no Danish/German/...
    Locale.setDefault(Locale.US);

    vertx = Vertx.vertx();

    log.info("Starting kafka cluster");
    KAFKA_CLUSTER = EmbeddedKafkaCluster.provisionWith(defaultClusterConfig());
    KAFKA_CLUSTER.start();
    String[] hostAndPort = KAFKA_CLUSTER.getBrokerList().split(":");
    System.setProperty(KAFKA_HOST, hostAndPort[0]);
    System.setProperty(KAFKA_PORT, hostAndPort[1]);
    System.setProperty(KAFKA_ENV, KAFKA_ENV_VALUE);
    System.setProperty(OKAPI_URL_KEY, "http://localhost:" + MOCK_KAFKA_PORT);
    log.info("Kafka cluster started with broker list: {}", KAFKA_CLUSTER.getBrokerList());

    log.info("Starting container database");
    PostgresClient.setPostgresTester(new PostgresTesterContainer());

    DeploymentOptions options = new DeploymentOptions();
    options.setConfig(new JsonObject().put("http.port", port));
    options.setWorker(true);
    startVerticle(options);

    tenantJob = prepareTenant(TENANT_HEADER, false, false);
  }

  @AfterAll
  public static void after() throws InterruptedException, ExecutionException, TimeoutException {
    log.info("Delete tenant");
    KAFKA_CLUSTER.stop();
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
    log.info("Stop database");
    PostgresClient.stopPostgresTester();
  }

  private static void startVerticle(DeploymentOptions options)
    throws InterruptedException, ExecutionException, TimeoutException {

    log.info("Start verticle");

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

  @SneakyThrows
  public static List<String> checkKafkaEventSent(String tenant, String eventType, int expected, String userId) {
    String topicToObserve = formatToKafkaTopicName(tenant, eventType);
    return KAFKA_CLUSTER.observeValues(ObserveKeyValues.on(topicToObserve, expected)
      .filterOnHeaders(val -> {
        var header = val.lastHeader(RestVerticle.OKAPI_USERID_HEADER.toLowerCase());
        if (Objects.nonNull(header)) {
          return new String(header.value()).equalsIgnoreCase(userId);
        }
        return false;
      })
      .observeFor(30, TimeUnit.SECONDS)
      .build());
  }

  private static String formatToKafkaTopicName(String tenant, String eventType) {
    return KafkaTopicNameHelper.formatTopicName(KAFKA_ENV_VALUE, getDefaultNameSpace(), tenant, eventType);
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
  class HelperUtilsTestNested extends HelperUtilsTest {}
  @Nested
  class ExportConfigCredentialsTestNested extends ExportConfigCredentialsTest {}
  @Nested
  class BatchVoucherTestNested extends BatchVoucherTest{}
  @Nested
  class BatchVoucherExportsTestNested extends BatchVoucherExportsImplTest{}
  @Nested
  class VoucherNumberTestNested extends VoucherNumberTest {}
  @Nested
  class OrderStorageServiceTestNested extends OrderStorageServiceTest {}
  @Nested
  class AuditOutboxAPITestNested extends AuditOutboxAPITest {}
}
