package org.folio.service;

import static java.util.stream.Collectors.toList;
import static one.util.streamex.StreamEx.ofSubLists;
import static org.folio.rest.exception.ErrorCodes.ORDER_RELATES_TO_INVOICE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationshipCollection;
import org.folio.rest.acq.model.orders.PurchaseOrder;
import org.folio.rest.acq.model.orders.PurchaseOrderCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.model.RequestContext;
import org.folio.rest.core.model.RequestEntry;

import org.folio.rest.exception.HttpException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.service.util.CommonServiceUtil.convertIdsToCqlQuery;
import static org.folio.service.util.CommonServiceUtil.collectResultsOnSuccess;

public class OrdersStorageService {

  private static final Logger logger = LogManager.getLogger(OrdersStorageService.class);


  public static final int MAX_IDS_FOR_GET_RQ = 15;
  private static final String PURCHASE_ORDERS_ENDPOINT = "/orders-storage/purchase-orders";
  private static final String PURCHASE_ORDER_BY_ID_ENDPOINT = "/orders-storage/purchase-orders/{id}";
  private static final String ORDER_INVOICE_RELATIONSHIP_ENDPOINT = "/orders-storage/order-invoice-relns";

  private final RestClient restClient;

  public OrdersStorageService(RestClient restClient) {
    this.restClient = restClient;
  }

  public CompletableFuture<List<PurchaseOrder>> retrievePurchaseOrdersByIdsInChunks(List<String> ids, RequestContext requestContext) {

    return collectResultsOnSuccess(ofSubLists(ids, MAX_IDS_FOR_GET_RQ)
      .map(id -> getPurchaseOrderByIds(id, requestContext)).toList())
      .thenApply(lists -> lists.stream()
        .flatMap(Collection::stream)
        .collect(toList()));
  }

  public CompletableFuture<List<PurchaseOrder>> getPurchaseOrderByIds(List<String> ids, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ids, "id");

    RequestEntry requestEntry = new RequestEntry(PURCHASE_ORDER_BY_ID_ENDPOINT)
      .withQuery(query)
      .withLimit(Integer.MAX_VALUE)
      .withOffset(0);

    return restClient.get(requestEntry, requestContext, PurchaseOrderCollection.class)
      .thenApply(PurchaseOrderCollection::getPurchaseOrders);
  }

  public CompletableFuture<PurchaseOrderCollection> getPurchaseOrders(String query, int limit, int offset, RequestContext requestContext) {
    RequestEntry requestEntry = new RequestEntry(PURCHASE_ORDERS_ENDPOINT)
      .withQuery(query)
      .withLimit(limit)
      .withOffset(offset);
    return restClient.get(requestEntry, requestContext, PurchaseOrderCollection.class);
  }

  public CompletableFuture<OrderInvoiceRelationshipCollection> getOrderInvoiceRelationshipCollection(String query, int offset, int limit, RequestContext requestContext) {
    RequestEntry requestEntry = new RequestEntry(ORDER_INVOICE_RELATIONSHIP_ENDPOINT).withQuery(query).withOffset(offset).withLimit(limit);
    return restClient.get(requestEntry, requestContext, OrderInvoiceRelationshipCollection.class);
  }

  //  Utility method(s):
  public Map<String, String> extractPurchaseOrderAndInvoiceIdIds (OrderInvoiceRelationshipCollection orderInvoiceRelationshipCollection) {
    Map<String, String> invoiceIdToPurchaseOrderId = new HashMap<>();

    orderInvoiceRelationshipCollection.getOrderInvoiceRelationships().forEach(orderInvoiceRelationship -> {
      invoiceIdToPurchaseOrderId.put(orderInvoiceRelationship.getPurchaseOrderId(), orderInvoiceRelationship.getInvoiceId());
    });

    return invoiceIdToPurchaseOrderId;
  }

  public CompletableFuture<Void> checkOrderInvoiceRelationship(String id, RequestContext requestContext) {
    String query = "purchaseOrderId==" + id;

    return getOrderInvoiceRelationshipCollection(query, 0,0, requestContext)
      .thenApply(oirs -> {
        if (oirs.getTotalRecords() > 0) {
          logger.error("Order or order line {} is linked to the invoice and can not be deleted", id);
          throw new HttpException(400, ORDER_RELATES_TO_INVOICE);
        }
        return null;
      });
  }

}
