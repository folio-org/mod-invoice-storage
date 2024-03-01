package org.folio.rest.utils;

import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.ModuleId;
import org.folio.okapi.common.SemVer;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.CriterionBuilder;
import org.folio.rest.persist.DBClient;

public class MigrationUtils {
  private static final Logger log = LogManager.getLogger(HelperUtils.class);

  private MigrationUtils() {
  }

  public static Future<Void> migration(TenantAttributes attributes, String migrationModule, Supplier<Future<Void>> supplier) {
    if (StringUtils.isBlank(attributes.getModuleFrom()))
      return Future.succeededFuture();

    SemVer moduleTo = moduleVersionToSemVer(migrationModule);
    SemVer currentModuleVersion = moduleVersionToSemVer(attributes.getModuleFrom());
    if (moduleTo.compareTo(currentModuleVersion) > 0) {
      return supplier.get();
    }
    return Future.succeededFuture();
  }

  public static SemVer moduleVersionToSemVer(String version) {
    try {
      return new SemVer(version);
    } catch (IllegalArgumentException ex) {
      return new ModuleId(version).getSemVer();
    }
  }

  public static Future<Void> populateInvoiceWithFiscalYear(Map<String, String> headers, Context vertxContext) {
    Promise<Void> promise = Promise.promise();
    vertxContext.runOnContext(event -> {
      DBClient client = new DBClient(vertxContext, headers);
      RequestContext requestContext = new RequestContext(vertxContext, headers);
      client.startTx()
        .compose(v -> getInvoicesWithoutFiscalYearFromDb(client))
        .compose(invoices -> getTransactionsByInvoiceIds(invoices, requestContext))
        .compose(invoices -> updateInvoicesWithFiscalYearId(client, invoices))
        .compose(v -> client.endTx())
        .onSuccess(v -> {
          log.info("ok");
          promise.complete();
        })
        .onFailure(v -> {
          log.info("Some error");
          promise.fail(("Some error"));
        });
    });
    return promise.future();
  }

  private static Future<List<Invoice>> getInvoicesWithoutFiscalYearFromDb(DBClient client) {
    Promise<List<Invoice>> promise = Promise.promise();
    Criteria criteria = new Criteria();
    criteria.addField("'fiscalYearId'");
    criteria.setOperation("IS NULL");
    Criterion criterion = new CriterionBuilder().build();
    criterion.addCriterion(criteria);

    client.getPgClient().get(INVOICE_TABLE, Invoice.class, criterion, false, reply -> {
      if (reply.failed()) {
        promise.fail("error");
      } else {
        log.info("Transaction record {} was successfully retrieved", reply.result());
        List<Invoice> invoices = reply.result().getResults();
        promise.complete(invoices);
      }
    });
    return promise.future();
  }

  private static Future<Void> updateInvoicesWithFiscalYearId(DBClient dbClient, List<Invoice> invoices) {
    if (invoices.isEmpty()) {
      return Future.succeededFuture();
    }

    Promise<Void> promise = Promise.promise();
    for (Invoice invoice : invoices) {
      dbClient.getPgClient().update(INVOICE_TABLE, invoice, invoice.getId(), reply -> {
        if (reply.failed()) {
          log.error("Error when updating invoice '{}'", invoice.getId());
          promise.fail(reply.cause());
        }
      });
    }
    promise.complete();
    return promise.future();
  }

  private static Future<List<Invoice>> getTransactionsByInvoiceIds(List<Invoice> invoices, RequestContext requestContext) {
    if (invoices.isEmpty()) {
      return Future.succeededFuture();
    }
    Promise<List<Invoice>> promise = Promise.promise();
    RestClient restClient = new RestClient();
    // Extract the invoice IDs from the list of invoices
    List<String> invoiceIds = invoices.stream()
      .map(Invoice::getId)
      .toList();

    // Build a query string using the 'IN' operator
    String query = "(sourceInvoiceId==" + "(" + String.join(" or ", invoiceIds.stream().map(id -> "\"*" + id + "*\"").toList()) + "))";
    RequestEntry requestEntry = new RequestEntry("/finance-storage/transactions")
      .withOffset(0)
      .withQuery(query)
      .withLimit(Integer.MAX_VALUE);

    restClient.get(requestEntry, requestContext)
      .thenApply(response -> {
        JsonArray transactions = response.getJsonArray("transactions");
        Map<String, String> invoiceIdToFiscalYearMap = new HashMap<>();

        // Iterate over the transactions and map invoice IDs to fiscal year IDs
        for (int i = 0; i < transactions.size(); i++) {
          JsonObject transaction = transactions.getJsonObject(i);
          String invoiceId = transaction.getString("sourceInvoiceId");
          String fiscalYearId = transaction.getString("fiscalYearId");
          invoiceIdToFiscalYearMap.put(invoiceId, fiscalYearId);
        }

        // Update the invoices with the fiscal year IDs
        invoices.forEach(invoice -> {
          String fiscalYearId = invoiceIdToFiscalYearMap.get(invoice.getId());
          invoice.setFiscalYearId(fiscalYearId); // Assuming Invoice has a setFiscalYearId method
        });

        promise.complete(invoices);
        return Future.succeededFuture();
      })
      .exceptionally(e -> {
        promise.fail(e.getCause());
        return Future.failedFuture(e);
      });

    return promise.future();
  }

}
