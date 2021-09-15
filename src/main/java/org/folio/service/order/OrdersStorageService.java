package org.folio.service.order;

import static java.util.stream.Collectors.toList;
import static one.util.streamex.StreamEx.ofSubLists;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationshipCollection;
import org.folio.rest.acq.model.orders.PurchaseOrder;
import org.folio.rest.acq.model.orders.PurchaseOrderCollection;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.folio.service.util.CommonServiceUtil.convertIdsToCqlQuery;
import static org.folio.service.util.CommonServiceUtil.collectResultsOnSuccess;

public class OrdersStorageService {

  private static final Logger logger = LogManager.getLogger(OrdersStorageService.class);

  public static final int MAX_IDS_FOR_GET_RQ = 15;
  private static final String PURCHASE_ORDER_BY_ID_ENDPOINT = "/orders-storage/purchase-orders/{id}";
  private static final String ORDER_INVOICE_RELATIONSHIP_ENDPOINT = "/orders-storage/order-invoice-relns";

  private final RestClient restClient;

  public OrdersStorageService(RestClient restClient) {
    this.restClient = restClient;
  }

  public CompletableFuture<List<PurchaseOrder>> getPurchaseOrderByIds(List<String> ids, RequestContext requestContext) {

    return collectResultsOnSuccess(ofSubLists(ids, MAX_IDS_FOR_GET_RQ)
      .map(chunkIds -> getOrdersChunk(chunkIds, requestContext)).toList())
      .thenApply(lists -> lists.stream()
        .flatMap(Collection::stream)
        .collect(toList()));
  }

  private CompletableFuture<List<PurchaseOrder>> getOrdersChunk(List<String> ids, RequestContext requestContext) {
    String query = convertIdsToCqlQuery(ids, "id");

    RequestEntry requestEntry = new RequestEntry(PURCHASE_ORDER_BY_ID_ENDPOINT)
      .withQuery(query)
      .withLimit(Integer.MAX_VALUE)
      .withOffset(0);

    return restClient.get(requestEntry, requestContext, PurchaseOrderCollection.class)
      .thenApply(PurchaseOrderCollection::getPurchaseOrders);
  }

  public CompletableFuture<OrderInvoiceRelationshipCollection> getOrderInvoiceRelationshipCollection(RequestContext requestContext) {
    RequestEntry requestEntry = new RequestEntry(ORDER_INVOICE_RELATIONSHIP_ENDPOINT)
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE);
    return restClient.get(requestEntry, requestContext, OrderInvoiceRelationshipCollection.class);
  }

}
