package org.folio.service;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.okapi.common.WebClientFactory;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.Tuple;

public class ConfigurationMigrationServiceTest {

  private ConfigurationMigrationService service;

  @Mock
  private WebClient webClient;
  @Mock
  private HttpRequest<Buffer> httpRequest;
  @Mock
  private HttpResponse<Buffer> httpResponse;
  @Mock
  private PostgresClient pgClient;
  @Mock
  private Context vertxContext;
  @Mock
  private Vertx vertx;

  private MockedStatic<WebClientFactory> webClientFactoryMock;
  private MockedStatic<PostgresClient> postgresClientMock;

  private Map<String, String> headers;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    service = new ConfigurationMigrationService();

    when(vertxContext.owner()).thenReturn(vertx);

    headers = new HashMap<>();
    headers.put("x-okapi-url", "http://localhost:9130");
    headers.put(OKAPI_HEADER_TENANT, "diku");

    webClientFactoryMock = mockStatic(WebClientFactory.class);
    webClientFactoryMock.when(() -> WebClientFactory.getWebClient(any(Vertx.class), any()))
      .thenReturn(webClient);
    when(webClient.getAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeaders(any())).thenReturn(httpRequest);

    postgresClientMock = mockStatic(PostgresClient.class);
    postgresClientMock.when(() -> PostgresClient.getInstance(any(Vertx.class), anyString()))
      .thenReturn(pgClient);
  }

  @AfterEach
  void tearDown() {
    webClientFactoryMock.close();
    postgresClientMock.close();
  }

  @Test
  void shouldSkipMigrationOnFreshInstall() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleTo("mod-invoice-storage-6.1.0");

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    webClientFactoryMock.verify(
      () -> WebClientFactory.getWebClient(any(Vertx.class), any()), never());
  }

  @Test
  void shouldSkipMigrationWhenAlreadyAtTargetVersion() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-6.1.0")
      .withModuleTo("mod-invoice-storage-6.2.0");

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    webClientFactoryMock.verify(
      () -> WebClientFactory.getWebClient(any(Vertx.class), any()), never());
  }

  @Test
  void shouldSkipMigrationWhenPastTargetVersion() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-7.0.0")
      .withModuleTo("mod-invoice-storage-7.1.0");

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    webClientFactoryMock.verify(
      () -> WebClientFactory.getWebClient(any(Vertx.class), any()), never());
  }

  @Test
  void shouldSkipMigrationWhenModuleToIsBelowTargetVersion() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.0.0");

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    webClientFactoryMock.verify(
      () -> WebClientFactory.getWebClient(any(Vertx.class), any()), never());
  }

  @Test
  void shouldMigrateWhenUpgradingFromSnapshotVersion() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-6.0.0-SNAPSHOT.123")
      .withModuleTo("mod-invoice-storage-6.1.0");

    mockHttpResponse(200, new JsonObject()
      .put("configs", new JsonArray())
      .put("totalRecords", 0));

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    webClientFactoryMock.verify(
      () -> WebClientFactory.getWebClient(any(Vertx.class), any()), times(1));
  }

  @Test
  void shouldSkipMigrationWhenNoOkapiUrl() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    Map<String, String> headersNoUrl = new HashMap<>();
    headersNoUrl.put(OKAPI_HEADER_TENANT, "diku");

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headersNoUrl, vertxContext);

    assertTrue(result.succeeded());
    webClientFactoryMock.verify(
      () -> WebClientFactory.getWebClient(any(Vertx.class), any()), never());
  }

  @Test
  void shouldMigrateSettingsFromConfiguration() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    String settingId = UUID.randomUUID().toString();
    JsonObject configEntry = new JsonObject()
      .put("id", settingId)
      .put("module", "INVOICE")
      .put("configName", "ROUTING_ADDRESS")
      .put("value", "some-address-uuid")
      .put("metadata", new JsonObject().put("createdDate", "2024-01-01"));

    mockHttpResponse(200, new JsonObject()
      .put("configs", new JsonArray().add(configEntry))
      .put("totalRecords", 1));
    mockPgExecuteSuccess();

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(pgClient).execute(sqlCaptor.capture(), any(Tuple.class));

    String sql = sqlCaptor.getValue();
    assertTrue(sql.contains("INSERT INTO settings"));
    assertTrue(sql.contains("ROUTING_ADDRESS"));
    assertTrue(sql.contains("some-address-uuid"));
    assertTrue(sql.contains("ON CONFLICT"));
  }

  @Test
  void shouldMigrateAdjustmentPresetsFromConfiguration() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    String presetId = UUID.randomUUID().toString();
    JsonObject presetValue = new JsonObject()
      .put("description", "Shipping")
      .put("exportToAccounting", false)
      .put("prorate", "Not prorated")
      .put("relationToTotal", "In addition to")
      .put("type", "Amount")
      .put("alwaysShow", false)
      .put("defaultAmount", 5);

    JsonObject configEntry = new JsonObject()
      .put("id", presetId)
      .put("module", "INVOICE")
      .put("configName", "INVOICE.adjustments")
      .put("value", presetValue.encode())
      .put("metadata", new JsonObject().put("createdDate", "2024-01-01"));

    mockHttpResponse(200, new JsonObject()
      .put("configs", new JsonArray().add(configEntry))
      .put("totalRecords", 1));
    mockPgExecuteSuccess();

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(pgClient).execute(sqlCaptor.capture(), any(Tuple.class));

    String sql = sqlCaptor.getValue();
    assertTrue(sql.contains("INSERT INTO adjustment_presets"));
    assertTrue(sql.contains("Shipping"));
    assertTrue(sql.contains("ON CONFLICT"));
  }

  @Test
  void shouldMigrateBothSettingsAndAdjustmentPresets() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    JsonObject settingEntry = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("module", "INVOICE")
      .put("configName", "ROUTING_ADDRESS")
      .put("value", "some-value")
      .put("metadata", new JsonObject());

    JsonObject presetEntry = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("module", "INVOICE")
      .put("configName", "INVOICE.adjustments")
      .put("value", new JsonObject().put("description", "Tax").encode())
      .put("metadata", new JsonObject());

    mockHttpResponse(200, new JsonObject()
      .put("configs", new JsonArray().add(settingEntry).add(presetEntry))
      .put("totalRecords", 2));
    mockPgExecuteSuccess();

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    verify(pgClient, times(2)).execute(anyString(), any(Tuple.class));
  }

  @Test
  void shouldHandleEmptyConfigurationResponse() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    mockHttpResponse(200, new JsonObject()
      .put("configs", new JsonArray())
      .put("totalRecords", 0));

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    verify(pgClient, never()).execute(anyString(), any(Tuple.class));
  }

  @Test
  void shouldRecoverWhenHttpCallFails() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    when(httpRequest.send())
      .thenReturn(Future.failedFuture(new RuntimeException("Connection refused")));

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    verify(pgClient, never()).execute(anyString(), any(Tuple.class));
  }

  @Test
  void shouldRecoverWhenHttpResponseIsError() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    mockHttpResponse(403, new JsonObject());

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
    verify(pgClient, never()).execute(anyString(), any(Tuple.class));
  }

  @Test
  void shouldRecoverWhenDbInsertFails() {
    TenantAttributes attributes = new TenantAttributes()
      .withModuleFrom("mod-invoice-storage-5.0.0")
      .withModuleTo("mod-invoice-storage-6.1.0");

    JsonObject configEntry = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("module", "INVOICE")
      .put("configName", "ROUTING_ADDRESS")
      .put("value", "some-value")
      .put("metadata", new JsonObject());

    mockHttpResponse(200, new JsonObject()
      .put("configs", new JsonArray().add(configEntry))
      .put("totalRecords", 1));

    when(pgClient.execute(anyString(), any(Tuple.class)))
      .thenReturn(Future.failedFuture(new RuntimeException("DB connection error")));

    Future<Void> result = service.migrateConfigurationData(attributes, "diku", headers, vertxContext);

    assertTrue(result.succeeded());
  }

  private void mockHttpResponse(int statusCode, JsonObject body) {
    when(httpResponse.statusCode()).thenReturn(statusCode);
    when(httpResponse.bodyAsJsonObject()).thenReturn(body);
    when(httpRequest.send()).thenReturn(Future.succeededFuture(httpResponse));
  }

  private void mockPgExecuteSuccess() {
    when(pgClient.execute(anyString(), any(Tuple.class)))
      .thenReturn(Future.succeededFuture());
  }
}
