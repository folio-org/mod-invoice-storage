package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.StorageTestSuite.autowireDependencies;
import static org.folio.rest.impl.StorageTestSuite.initSpringContext;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.postTenant;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.folio.rest.core.RestClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.utils.TenantApiTestUtil;
import org.folio.service.migration.MigrationService;
import org.folio.service.order.OrdersStorageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import io.restassured.http.Header;

class TenantReferenceAPITest extends TestBase {

  private static final Header MIGRATION_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "migration_tenant");

  private static TenantJob tenantJob;

  @Autowired
  private MigrationService migrationService;

  @BeforeEach
  void initMocks() {
    autowireDependencies(this);
  }

  @BeforeAll
  public static void before()  {
    initSpringContext(ContextConfiguration.class);
  }

  @AfterEach
  void resetMocks() {
    reset(migrationService);
  }

  @AfterAll
  public static void after() {
    deleteTenant(tenantJob, MIGRATION_TENANT_HEADER);
  }

  @ParameterizedTest
  @CsvSource({
    "mod-invoice-storage-5.0.0,1",
    "mod-invoice-storage-5.2.0,0",
    ",0"
  })
  void testToVerifyWhetherMigrationForDifferentVersionShouldRun(String version, Integer times) {
    TenantAttributes tenantAttributes = TenantApiTestUtil.prepareTenantBody(false, false);
    tenantAttributes.setModuleFrom(version);
    tenantJob = postTenant(MIGRATION_TENANT_HEADER, tenantAttributes);
    verify(migrationService, times(times)).syncOrderPoNumbersWithInvoicePoNumbers(any(), any());
  }

  public static class ContextConfiguration {

    @Bean
    MigrationService migrationService(OrdersStorageService ordersStorageService) {
      return mock(MigrationService.class);
    }

    @Bean
    OrdersStorageService ordersStorageService(RestClient restClient) {
      return mock(OrdersStorageService.class);
    }

    @Bean
    RestClient restClient() {
      return mock(RestClient.class);
    }
  }
}
