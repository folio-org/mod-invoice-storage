package org.folio.migration;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.migration.models.dto.InvoiceUpdateDto;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationship;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationshipCollection;
import org.folio.rest.acq.model.orders.PurchaseOrder;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.order.OrderStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public
class MigrationServiceTest {

  public MigrationService migrationService;

  @Mock
  public OrderStorageService orderStorageService;

  @Mock
  public DBClient dbClient;

  @Mock
  private PostgresClient postgresClient;

  private Map<String, String> okapiHeaders = new HashMap<>();

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
    migrationService = Mockito.spy(new MigrationService(orderStorageService));
  }

  @Test
  void testShouldCompleteAddOrderPoNumberToInvoicePoNumberIfAllExecuted(VertxTestContext testContext) {
    Vertx vertx = Vertx.vertx();

    OrderInvoiceRelationshipCollection relations = new OrderInvoiceRelationshipCollection();
    String orderId1 = UUID.randomUUID().toString();
    PurchaseOrder order1 = new PurchaseOrder().withId(orderId1).withPoNumber("10000");
    String orderId2 = UUID.randomUUID().toString();
    PurchaseOrder order2 = new PurchaseOrder().withId(orderId2).withPoNumber("10001");
    String orderId3 = UUID.randomUUID().toString();
    PurchaseOrder order3 = new PurchaseOrder().withId(orderId3).withPoNumber("10002");
    String invoiceId1 = UUID.randomUUID().toString();
    String invoiceId2 = UUID.randomUUID().toString();
    OrderInvoiceRelationship relation1 = new OrderInvoiceRelationship().withId(UUID.randomUUID().toString())
                                                    .withInvoiceId(invoiceId1).withPurchaseOrderId(orderId1);
    OrderInvoiceRelationship relation2 = new OrderInvoiceRelationship().withId(UUID.randomUUID().toString())
                                                    .withInvoiceId(invoiceId1).withPurchaseOrderId(orderId2);
    OrderInvoiceRelationship relation3 = new OrderInvoiceRelationship().withId(UUID.randomUUID().toString())
                                                    .withInvoiceId(invoiceId2).withPurchaseOrderId(orderId1);
    OrderInvoiceRelationship relation4 = new OrderInvoiceRelationship().withId(UUID.randomUUID().toString())
                                                    .withInvoiceId(UUID.randomUUID().toString()).withPurchaseOrderId(orderId3);

    relations.withOrderInvoiceRelationships(List.of(relation1, relation2, relation3, relation4));
    when(orderStorageService.getOrderInvoiceRelationshipCollection(any()))
                             .thenReturn(completedFuture(relations));
    when(orderStorageService.getPurchaseOrdersByIds(any(), any()))
                             .thenReturn(completedFuture(List.of(order1, order2, order3)));

    doReturn(Future.succeededFuture()).when(migrationService).runScriptUpdateInvoicesWithPoNumbers(any(), any());

    testContext.assertComplete(migrationService.syncOrderPoNumbersWithInvoicePoNumbers(okapiHeaders, vertx.getOrCreateContext()))
      .onComplete(event -> {
        testContext.verify(() -> {
          verify(orderStorageService, times(1)).getOrderInvoiceRelationshipCollection(any());
          verify(orderStorageService, times(1)).getPurchaseOrdersByIds(any(), any());
        });
        testContext.completeNow();
      });
  }

  @Test
  void testShouldFailedIfGetOrderInvoiceRelationshipCollectionThrowException(VertxTestContext testContext) {
    Vertx vertx = Vertx.vertx();

    when(orderStorageService.getOrderInvoiceRelationshipCollection(any()))
      .thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
    when(orderStorageService.getPurchaseOrdersByIds(any(), any()))
      .thenReturn(completedFuture(Collections.emptyList()));

    doReturn(Future.succeededFuture()).when(migrationService).runScriptUpdateInvoicesWithPoNumbers(any(), any());

    testContext.assertFailure(migrationService.syncOrderPoNumbersWithInvoicePoNumbers(okapiHeaders, vertx.getOrCreateContext()))
      .onComplete(event -> {
        testContext.verify(() -> {
          verify(orderStorageService, times(1)).getOrderInvoiceRelationshipCollection(any());
          verify(orderStorageService, never()).getPurchaseOrdersByIds(any(), any());
        });
        testContext.completeNow();
      });
  }

  @Test
  void testShouldCallExecuteSuccessfullyRunScriptUpdateInvoicesWithPoNumbers(VertxTestContext testContext) {
    when(dbClient.getPgClient()).thenReturn(postgresClient);
    when(dbClient.getTenantId()).thenReturn("TEST");
    doAnswer((Answer<Void>) invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(1);
      handler.handle(Future.succeededFuture());
      return null;
    }).when(postgresClient)
      .execute(any(), any(Handler.class));

    testContext.assertComplete(migrationService.runScriptUpdateInvoicesWithPoNumbers(List.of(new InvoiceUpdateDto()), dbClient))
      .onComplete(event -> {
        testContext.verify(() -> {
          verify(postgresClient, times(1)).execute(any(), any(Handler.class));
        });
        testContext.completeNow();
      });
  }

  @Test
  void testShouldFailIfPgClientReturnException(VertxTestContext testContext) {
    when(dbClient.getPgClient()).thenReturn(postgresClient);
    when(dbClient.getTenantId()).thenReturn("TEST");
    doAnswer((Answer<Void>) invocation -> {
      Handler<AsyncResult<String>> handler = invocation.getArgument(1);
      handler.handle(Future.failedFuture(new RuntimeException()));
      return null;
    }).when(postgresClient)
      .execute(any(), any(Handler.class));

    testContext.assertFailure(migrationService.runScriptUpdateInvoicesWithPoNumbers(List.of(new InvoiceUpdateDto()), dbClient))
      .onComplete(event -> {
        testContext.verify(() -> {
          verify(postgresClient, times(1)).execute(any(), any(Handler.class));
        });
        testContext.completeNow();
      });
  }
}
