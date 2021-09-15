package org.folio.migration;

import static org.folio.rest.utils.ResponseUtils.handleFailure;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.migration.models.dto.InvoiceUpdateDto;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationship;
import org.folio.rest.acq.model.orders.OrderInvoiceRelationshipCollection;
import org.folio.rest.acq.model.orders.PurchaseOrder;
import org.folio.completablefuture.FolioVertxCompletableFuture;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.service.order.OrderStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MigrationService {

  private static final Logger log = LogManager.getLogger(MigrationService.class);
  public static final String MATCHING_PO_FROM_INVOICE_REL_AND_ORDER_TABLES_ERROR = "Purchase order was found in invoice_order relation table, but not in purchase order table";
  private final OrderStorageService orderStorageService;

  public MigrationService(OrderStorageService orderStorageService) {
    this.orderStorageService = orderStorageService;
  }

  public Future<Void> syncOrderPoNumbersWithInvoicePoNumbers(Map<String, String> headers, Context vertxContext) {
    Promise<Void> promise = Promise.promise();
    var requestContext = new RequestContext(vertxContext, headers);
    vertxContext.runOnContext(v -> {
      log.debug("Cross Migration for invoice PO number synchronization started");
      DBClient client = new DBClient(vertxContext, headers);
      orderStorageService.getOrderInvoiceRelationshipCollection(requestContext)
        .thenCompose(relationshipCollection -> mapRelationshipsByPoId(relationshipCollection, requestContext).thenCompose(
            map -> orderStorageService.getPurchaseOrdersByIds(new ArrayList<>(map.keySet()), requestContext)
              .thenApply(purchaseOrders -> convertToInvoiceUpdateDtoList(relationshipCollection, purchaseOrders))))
        .thenAccept(dtoList -> runScriptUpdateInvoicesWithPoNumbers(dtoList, client).onSuccess(v1 -> {
          log.debug("Cross Migration for invoice PO number synchronization completed");
          promise.complete();
        })
          .onFailure(v2 -> {
            log.error("Cross Migration for invoice PO number synchronization failed");
            promise.fail(v2.getCause());
          }))
        .exceptionally(throwable -> {
          log.error("Cross Migration for invoice PO number synchronization failed");
          promise.fail(throwable.getCause());
          return null;
        });

    });
    return promise.future();
  }

  private CompletableFuture<Map<String, List<OrderInvoiceRelationship>>> mapRelationshipsByPoId(
      OrderInvoiceRelationshipCollection relationshipCollection, RequestContext requestContext) {
    Map<String, List<OrderInvoiceRelationship>> map = relationshipCollection.getOrderInvoiceRelationships()
      .stream()
      .collect(Collectors.groupingBy(OrderInvoiceRelationship::getPurchaseOrderId));

    return FolioVertxCompletableFuture.from(requestContext.getContext(), CompletableFuture.completedFuture(map));
  }

  private List<InvoiceUpdateDto> convertToInvoiceUpdateDtoList(OrderInvoiceRelationshipCollection relationshipCollection,
      List<PurchaseOrder> purchaseOrders) {
    Map<String, List<OrderInvoiceRelationship>> map = relationshipCollection.getOrderInvoiceRelationships()
      .stream()
      .collect(Collectors.groupingBy(OrderInvoiceRelationship::getInvoiceId));

    List<InvoiceUpdateDto> dtoList = new ArrayList<>();

    map.forEach((key, value) -> dtoList.add(new InvoiceUpdateDto().withInvoiceId(key)
      .withPoNumbers(value.stream()
                          .map(relationship -> {
                            Optional<PurchaseOrder> order = purchaseOrders.stream()
                            .filter(po -> po.getId().equals(relationship.getPurchaseOrderId()))
                            .findAny();
                            if (order.isPresent()) {
                              return order.get().getPoNumber();
                            }
                            log.warn(MATCHING_PO_FROM_INVOICE_REL_AND_ORDER_TABLES_ERROR);
                            return null;
                          })
        .filter(Objects::nonNull)
        .map(this::replaceSingleQuote)
        .collect(Collectors.toList()))));

    return dtoList;
  }

  private String replaceSingleQuote(String inputString) {
    return inputString != null ? inputString.replace("'", "''") : null;
  }

  public Future<Void> runScriptUpdateInvoicesWithPoNumbers(List<InvoiceUpdateDto> dtoList, DBClient client) {
    Promise<Void> promise = Promise.promise();
    String schemaName = PostgresClient.convertToPsqlStandard(client.getTenantId());
    String sql = "DO\n" + "$$\n" + "begin\n" + " PERFORM %s.update_invoices_with_po_numbers('%s');\n" + "end;\n"
        + "$$ LANGUAGE plpgsql;";

    var jsonString = new JsonArray(dtoList).encode();

    client.getPgClient()
      .execute(String.format(sql, schemaName, jsonString), event -> {
        if (event.succeeded()) {
          promise.complete();
        } else {
          handleFailure(promise, event);
        }
      });
    return promise.future();
  }
}
