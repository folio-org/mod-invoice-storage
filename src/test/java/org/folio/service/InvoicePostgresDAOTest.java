package org.folio.service;

import io.vertx.junit5.VertxExtension;
import org.folio.dao.invoice.InvoicePostgresDAO;
import org.folio.models.exception.HttpException;
import org.folio.rest.jaxrs.model.Invoice;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.UUID;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import io.restassured.http.Header;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class InvoicePostgresDAOTest {
  static final String TEST_TENANT = "test_tenant";
  private static final Header TEST_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, TEST_TENANT);
  private InvoicePostgresDAO invoicePostgresDAO = new InvoicePostgresDAO();
  @Mock
  private DBClient client;
  @Mock
  private PostgresClient postgresClient;
  @Mock
  private Logger logger;
  private static TenantJob tenantJob;

  @BeforeEach
  public void initMocks() throws MalformedURLException {
    MockitoAnnotations.openMocks(this);
    invoicePostgresDAO = Mockito.mock(InvoicePostgresDAO.class, Mockito.CALLS_REAL_METHODS);
    tenantJob = prepareTenant(TEST_TENANT_HEADER, false, false);
  }

  @AfterEach
  void cleanupData() throws MalformedURLException {
    deleteTenant(tenantJob, TEST_TENANT_HEADER);
  }

  @Test
  void shouldFailWhenCreatingSequence(VertxTestContext testContext) {
    String id = UUID.randomUUID().toString();
    when(client.getPgClient()).thenReturn(postgresClient);
    doAnswer((Answer<Void>) invocation -> {
      Handler<AsyncResult<Results<Invoice>>> handler = invocation.getArgument(2);
      handler.handle(Future.failedFuture(new HttpException(500, "Error")));
      return null;
    }).when(postgresClient).getById(eq(INVOICE_TABLE), eq(id), any(Handler.class));


    testContext.assertFailure(invoicePostgresDAO.createSequence(id, client))
      .onComplete(ar -> {
        HttpException exception = (HttpException) ar.cause();
        testContext.verify(() -> {
          assertEquals(500, exception.getCode());
          assertEquals("Error", exception.getError().getMessage());
        });
        testContext.completeNow();
      });
  }

  @Test
  void shouldFailWhenGettingInvoiceDocument(VertxTestContext testContext) {
    String invoiceId = UUID.randomUUID().toString();
    String documentId = UUID.randomUUID().toString();
    when(client.getPgClient()).thenReturn(postgresClient);
    Criterion criterion = new Criterion().addCriterion(new Criteria().addField("id").setOperation("=").setVal(invoiceId).setJSONB(false));
    doAnswer((Answer<Void>) invocation -> {
      Handler<AsyncResult<Results<Invoice>>> handler = invocation.getArgument(4);
      handler.handle(Future.failedFuture(new HttpException(500, "Error")));
      return null;
    }).when(postgresClient).get(eq(INVOICE_TABLE), eq(Invoice.class), eq(criterion), eq(false), any(Handler.class));

    testContext.assertFailure(invoicePostgresDAO.getInvoiceDocument(invoiceId, documentId, client)
      .onComplete(ar -> {
        HttpException exception = (HttpException) ar.cause();
        testContext.verify(() -> {
          assertEquals(500, exception.getCode());
          assertEquals("Error", exception.getError().getMessage());
        });
        testContext.completeNow();
      }));
  }
}
