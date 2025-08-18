package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.StorageTestSuite.getVertx;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.postTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.folio.rest.utils.TenantApiTestUtil.purge;
import static org.folio.rest.utils.TestEntities.INVOICE_LINES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.InvoiceCollection;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineCollection;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.TenantApiTestUtil;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;


public class TenantSampleDataTest extends TestBase {

  private static final Logger log = LogManager.getLogger(TenantSampleDataTest.class);

  private static final Header NONEXISTENT_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "no_tenant");
  private static final Header ANOTHER_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "new_tenant");
  private static final Header PARTIAL_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "partial_tenant");
  TenantJob tenantJob;

  @BeforeAll
  static void createRequiredTables() throws IOException, ExecutionException, InterruptedException, TimeoutException {
    createTable("finance_schema.sql");
    createTable("order_schema.sql");
    createTable("configuration_schema.sql");
  }

  private static void createTable(String schemaName) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    InputStream tableInput = TenantSampleDataTest.class.getClassLoader().getResourceAsStream(schemaName);
    String sqlFile = IOUtils.toString(Objects.requireNonNull(tableInput), StandardCharsets.UTF_8);
    CompletableFuture<Void> schemaCreated = new CompletableFuture<>();
    PostgresClient.getInstance(getVertx()).runSQLFile(sqlFile, false)
      .onComplete(listAsyncResult -> schemaCreated.complete(null));
    schemaCreated.get(60, TimeUnit.SECONDS);
  }

  @Test
  public void sampleDataTests() throws MalformedURLException {
    try {
      log.info("-- create a tenant with no sample data --");
      prepareTenant(ANOTHER_TENANT_HEADER, false,false);
      log.info("-- upgrade the tenant with sample data, so that it will be inserted now --");
      upgradeTenantWithSampleDataLoad();
      log.info("-- upgrade the tenant again with no sample data, so the previously inserted data stays in tact --");
      upgradeTenantWithNoSampleDataLoad();
    }
    finally {
      deleteTenant(tenantJob, ANOTHER_TENANT_HEADER);
    }
  }


  @Test
  public void testPartialSampleDataLoading() throws MalformedURLException {
    log.info("load sample data");
    try {
      TenantAttributes tenantAttributes = TenantApiTestUtil.prepareTenantBody(true, false);
      tenantJob = postTenant(PARTIAL_TENANT_HEADER, tenantAttributes);

      InvoiceLineCollection invoiceLineCollection = getData(INVOICE_LINES.getEndpoint(), PARTIAL_TENANT_HEADER).then()
        .extract()
        .response()
        .as(InvoiceLineCollection.class);

      for (InvoiceLine invoiceLine : invoiceLineCollection.getInvoiceLines()) {
        deleteData(INVOICE_LINES.getEndpointWithId(), invoiceLine.getId(), PARTIAL_TENANT_HEADER).then()
          .log()
          .ifValidationFails()
          .statusCode(204);
      }

      tenantAttributes = TenantApiTestUtil.prepareTenantBody(true, true);
      tenantJob = postTenant(PARTIAL_TENANT_HEADER, tenantAttributes);

      for (TestEntities entity : TestEntities.getCollectableEntities()) {
        log.info("Test expected quantity for " + entity.name());
        verifyCollectionQuantity(entity.getEndpoint(), entity.getInitialQuantity(), PARTIAL_TENANT_HEADER);
      }
    } finally {
      PostgresClient oldClient = PostgresClient.getInstance(StorageTestSuite.getVertx(), ANOTHER_TENANT_HEADER.getValue());
      deleteTenant(tenantJob, ANOTHER_TENANT_HEADER);
      PostgresClient newClient = PostgresClient.getInstance(StorageTestSuite.getVertx(), ANOTHER_TENANT_HEADER.getValue());
      assertThat(oldClient, not(newClient));
    }
  }

  private void upgradeTenantWithSampleDataLoad() throws MalformedURLException {

    log.info("upgrading Module with sample");
    TenantAttributes tenantAttributes = TenantApiTestUtil.prepareTenantBody(true, false);
    tenantJob = postTenant(ANOTHER_TENANT_HEADER, tenantAttributes);

    for (TestEntities entity : TestEntities.getCollectableEntities()) {
      log.info("Test expected quantity for " + entity.name());
      ValidatableResponse response = verifyCollectionQuantity(entity.getEndpoint(), entity.getInitialQuantity(), ANOTHER_TENANT_HEADER);

      switch (entity) {
      case INVOICE: {
        response.extract()
          .as(InvoiceCollection.class)
          .getInvoices()
          .forEach(invoice -> {
            assertThat(invoice.getAdjustmentsTotal(), notNullValue());
            assertThat(invoice.getSubTotal(), notNullValue());
            assertThat(invoice.getTotal(), notNullValue());
          });
        break;
      }
      case INVOICE_LINES: {
        response.extract()
          .as(InvoiceLineCollection.class)
          .getInvoiceLines()
          .forEach(line -> {
            assertThat(line.getAdjustmentsTotal(), notNullValue());
            assertThat(line.getSubTotal(), notNullValue());
            assertThat(line.getTotal(), notNullValue());
          });
        break;
      }
      default:
        break;
      }
    }
  }

  private void upgradeTenantWithNoSampleDataLoad() throws MalformedURLException {

    log.info("upgrading Module without sample data");

    TenantAttributes tenantAttributes = TenantApiTestUtil.prepareTenantBody(false, false);
    tenantJob = postTenant(PARTIAL_TENANT_HEADER, tenantAttributes);

    for (TestEntities te : TestEntities.getCollectableEntities()) {
      verifyCollectionQuantity(te.getEndpoint(), te.getEstimatedSystemDataRecordsQuantity());
    }
  }


  @Test
  public void upgradeTenantWithNonExistentDb() throws MalformedURLException {
    log.info("upgrading Module for non existed tenant");
    TenantAttributes tenantAttributes = TenantApiTestUtil.prepareTenantBody(false, false);
    try {
      // RMB-331 the case if older version has no db schema
      tenantJob = postTenant(NONEXISTENT_TENANT_HEADER, tenantAttributes);

      // Check that no sample data loaded
      for (TestEntities entity : TestEntities.getCollectableEntities()) {
        log.info("Test expected zero quantity for {}", entity.name());
        verifyCollectionQuantity(entity.getEndpoint(), TestEntities.BATCH_GROUP.equals(entity) ? 1 : 0, NONEXISTENT_TENANT_HEADER);
      }
    } finally {
      purge(NONEXISTENT_TENANT_HEADER);
    }
  }

}
