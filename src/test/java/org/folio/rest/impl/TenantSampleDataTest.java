package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.folio.rest.utils.TenantApiTestUtil.TENANT_ENDPOINT;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.postToTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.folio.rest.utils.TestEntities.INVOICE_LINES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.net.MalformedURLException;

import org.folio.rest.jaxrs.model.InvoiceCollection;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineCollection;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.TenantApiTestUtil;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class TenantSampleDataTest extends TestBase{

  private final Logger logger = LoggerFactory.getLogger(TenantSampleDataTest.class);

  private static final Header NONEXISTENT_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "no_tenant");
  private static final Header ANOTHER_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "new_tenant");
  private static final Header PARTIAL_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "partial_tenant");


  @Test
  public void isTenantCreated() throws MalformedURLException {
    getData(TENANT_ENDPOINT)
      .then()
      .assertThat()
      .statusCode(200);
  }

  @Test
  public void sampleDataTests() throws MalformedURLException {
    try {
      logger.info("-- create a tenant with no sample data --");
      prepareTenant(ANOTHER_TENANT_HEADER, false);
      logger.info("-- upgrade the tenant with sample data, so that it will be inserted now --");
      upgradeTenantWithSampleDataLoad();
      logger.info("-- upgrade the tenant again with no sample data, so the previously inserted data stays in tact --");
      upgradeTenantWithNoSampleDataLoad();
    }
    finally {
      deleteTenant(ANOTHER_TENANT_HEADER);
    }
  }

  @Test
  public void failIfNoUrlToHeader() throws MalformedURLException {
    JsonObject jsonBody = TenantApiTestUtil.prepareTenantBody(true, false);
    given()
      .header(new Header(OKAPI_HEADER_TENANT, "noURL"))
      .contentType(ContentType.JSON)
      .body(jsonBody.encodePrettily())
      .post(storageUrl(TENANT_ENDPOINT))
      .then()
      .assertThat()
      .statusCode(500);
  }

  @Test
  public void testPartialSampleDataLoading() throws MalformedURLException {
    logger.info("load sample data");
    try{
      JsonObject jsonBody = TenantApiTestUtil.prepareTenantBody(true, false);
      postToTenant(PARTIAL_TENANT_HEADER, jsonBody)
        .assertThat()
        .statusCode(201);
      InvoiceLineCollection invoiceLineCollection = getData(INVOICE_LINES.getEndpoint(), PARTIAL_TENANT_HEADER)
        .then()
        .extract()
        .response()
        .as(InvoiceLineCollection.class);

      for (InvoiceLine invoiceLine : invoiceLineCollection.getInvoiceLines()) {
        deleteData(INVOICE_LINES.getEndpointWithId(), invoiceLine.getId(), PARTIAL_TENANT_HEADER).then()
          .log()
          .ifValidationFails()
          .statusCode(204);
      }

      jsonBody = TenantApiTestUtil.prepareTenantBody(true, true);
      postToTenant(PARTIAL_TENANT_HEADER, jsonBody)
        .assertThat()
        .statusCode(201);

      for (TestEntities entity : TestEntities.values()) {
        logger.info("Test expected quantity for " + entity.name());
        verifyCollectionQuantity(entity.getEndpoint(), entity.getInitialQuantity(), PARTIAL_TENANT_HEADER);
      }
    } finally {
      PostgresClient oldClient = PostgresClient.getInstance(StorageTestSuite.getVertx(), PARTIAL_TENANT_HEADER.getValue());
      deleteTenant(PARTIAL_TENANT_HEADER);
      PostgresClient newClient = PostgresClient.getInstance(StorageTestSuite.getVertx(), PARTIAL_TENANT_HEADER.getValue());
      assertThat(oldClient, not(newClient));
    }
  }

  private void upgradeTenantWithSampleDataLoad() throws MalformedURLException {

    logger.info("upgrading Module with sample");
    JsonObject jsonBody = TenantApiTestUtil.prepareTenantBody(true, false);
    postToTenant(ANOTHER_TENANT_HEADER, jsonBody)
      .assertThat()
      .statusCode(201);
    for (TestEntities entity : TestEntities.values()) {
      logger.info("Test expected quantity for " + entity.name());
      ValidatableResponse response = verifyCollectionQuantity(entity.getEndpoint(), entity.getInitialQuantity(),
          ANOTHER_TENANT_HEADER);

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

    logger.info("upgrading Module without sample data");

    JsonObject jsonBody = TenantApiTestUtil.prepareTenantBody(false, false);
    postToTenant(ANOTHER_TENANT_HEADER, jsonBody);

    for(TestEntities te: TestEntities.values()){
      verifyCollectionQuantity(te.getEndpoint(), te.getSystemDataQuantity());
    }
  }


  @Test
  public void upgradeTenantWithNonExistentDb() throws MalformedURLException {
    logger.info("upgrading Module for non existed tenant");

    JsonObject jsonBody = TenantApiTestUtil.prepareTenantBody(false, false);
    try {
      // RMB-331 the case if older version has no db schema
      postToTenant(NONEXISTENT_TENANT_HEADER, jsonBody)
        .assertThat()
        .statusCode(201);

      // Check that no sample data loaded
      for (TestEntities entity : TestEntities.values()) {
        logger.info("Test expected quantity for " , 0, entity.name());
        verifyCollectionQuantity(entity.getEndpoint(), TestEntities.BATCH_GROUP.equals(entity)? 1 : 0, NONEXISTENT_TENANT_HEADER);
      }
    }
    finally {
      deleteTenant(NONEXISTENT_TENANT_HEADER);
    }
  }

}
