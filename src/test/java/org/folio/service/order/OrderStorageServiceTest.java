package org.folio.service.order;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.orders.OrderInvoiceRelationship;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationshipCollection;
import org.folio.rest.acq.model.orders.PurchaseOrder;
import org.folio.rest.acq.model.orders.PurchaseOrderCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OrderStorageServiceTest {
  @InjectMocks
  private OrderStorageService orderStorageService;
  @Mock
  private RestClient restClientMock;
  @Mock
  private RequestContext requestContextMock;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void successRetrievePurchaseOrdersByIds() {
    String orderId = UUID.randomUUID().toString();
    List<PurchaseOrder> purchaseOrders = Collections.singletonList(new PurchaseOrder()
      .withId(orderId));

    PurchaseOrderCollection purchaseOrderCollection = new PurchaseOrderCollection()
      .withPurchaseOrders(purchaseOrders)
      .withTotalRecords(1);

    when(restClientMock.get(any(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(purchaseOrderCollection));

    List<PurchaseOrder> actOrders = orderStorageService.getPurchaseOrdersByIds(List.of(orderId), requestContextMock).join();

    verify(restClientMock).get(any(), eq(requestContextMock), eq(PurchaseOrderCollection.class));
    assertEquals(purchaseOrderCollection.getPurchaseOrders(), actOrders);
  }

  @Test
  void shouldReturnOrderInvoiceRelationshipByInvoiceIdAndLineIdIfRelationExist() {
    String invoiceId = UUID.randomUUID().toString();
    String orderId = UUID.randomUUID().toString();
    OrderInvoiceRelationship relationship = new OrderInvoiceRelationship().withInvoiceId(invoiceId).withPurchaseOrderId(orderId);
    OrderInvoiceRelationshipCollection relationships = new OrderInvoiceRelationshipCollection().withOrderInvoiceRelationships(List.of(relationship)).withTotalRecords(1);

    doReturn(completedFuture(relationships)).when(restClientMock).get(any(RequestEntry.class), eq(requestContextMock), eq(OrderInvoiceRelationshipCollection.class));
    orderStorageService.getOrderInvoiceRelationshipCollection(requestContextMock).join();

    verify(restClientMock).get(any(RequestEntry.class), eq(requestContextMock), eq(OrderInvoiceRelationshipCollection.class));
  }
}
