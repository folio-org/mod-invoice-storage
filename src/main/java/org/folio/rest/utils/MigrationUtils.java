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
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.CriterionBuilder;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;

public class MigrationUtils {
  private static final Logger log = LogManager.getLogger(HelperUtils.class);
  private static final String TRANSACTION_API = "/finance-storage/transactions";

  private MigrationUtils() {
  }

  public static Future<Void> migrate(TenantAttributes attributes, String migrationModule, Supplier<Future<Void>> supplier) {
    log.debug("migrate:: Migrating module: moduleTo={}, migrationModule={}", attributes.getModuleTo(), migrationModule);
    if (StringUtils.isBlank(attributes.getModuleFrom()))
      return Future.succeededFuture();

    SemVer moduleTo = moduleVersionToSemVer(migrationModule);
    SemVer currentModuleVersion = moduleVersionToSemVer(attributes.getModuleFrom());
    if (moduleTo.compareTo(currentModuleVersion) > 0) {
      log.info("migrate:: moduleTo={} was greater than currentModuleVersion={} version", moduleTo, currentModuleVersion);
      return supplier.get();
    }

    log.info("migrate:: moduleTo={} was less or equals to currentModuleVersion={}", moduleTo, currentModuleVersion);
    return Future.succeededFuture();
  }

  public static SemVer moduleVersionToSemVer(String version) {
    try {
      return new SemVer(version);
    } catch (IllegalArgumentException ex) {
      return new ModuleId(version).getSemVer();
    }
  }

  public static Future<Void> updateInvoiceWithFiscalYear(Map<String, String> headers, Context vertxContext) {
    DBClient dbClient = new DBClient(vertxContext, headers);
    RequestContext requestContext = new RequestContext(vertxContext, headers);
    PostgresClient pgClient = dbClient.getPgClient();
    return pgClient.withTrans(conn -> getInvoicesWithoutFiscalYearFromDb(conn)
      .compose(invoices -> getTransactionsByInvoiceIds(invoices, requestContext))
      .compose(invoices -> updateInvoicesWithFiscalYearId(invoices, conn))
      .onSuccess(v -> log.info("updateInvoiceWithFiscalYear:: Successfully invoices were updated with fiscalYear"))
      .onFailure(t -> log.info("Error to update invoices with fiscalYear", t)));
  }

  private static Future<List<Invoice>> getInvoicesWithoutFiscalYearFromDb(Conn conn) {
    Criteria criteria = new Criteria();
    criteria.addField("'fiscalYearId'");
    criteria.setOperation("IS NULL");
    Criterion criterion = new CriterionBuilder().build();
    criterion.addCriterion(criteria);

    return conn.get(INVOICE_TABLE, Invoice.class, criterion, false)
      .map(Results::getResults)
      .onFailure(t -> log.error("Failed to fetch invoice with query={}", criterion.toString(), t));
  }

  private static Future<List<Invoice>> getTransactionsByInvoiceIds(List<Invoice> invoices, RequestContext requestContext) {
    if (invoices.isEmpty()) {
      log.info("getTransactionsByInvoiceIds:: invoices is empty");
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
    RequestEntry requestEntry = new RequestEntry(TRANSACTION_API)
      .withOffset(0)
      .withQuery(query)
      .withLimit(Integer.MAX_VALUE);

    log.info("getTransactionsByInvoiceIds:: Getting transaction by calling to finance-storage module: query={}", query);
    restClient.get(requestEntry, requestContext)
      .thenApply(response -> {
        JsonArray transactions = response.getJsonArray("transactions");
        Map<String, String> invoiceIdToFiscalYearMap = new HashMap<>();
        log.info("getTransactionsByInvoiceIds:: Retrieving transactions={} by for invoiceIds={}", invoiceIdToFiscalYearMap, invoiceIds);

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
        return Future.succeededFuture(invoices);
      })
      .exceptionally(e -> {
        log.info("Error to get transactions with query={} and api={}", query, TRANSACTION_API);
        promise.fail(e.getCause());
        return Future.failedFuture(e);
      });

    return promise.future();
  }

  private static Future<Void> updateInvoicesWithFiscalYearId(List<Invoice> invoices, Conn conn) {
    if (invoices.isEmpty()) {
      return Future.succeededFuture();
    }

    Promise<Void> promise = Promise.promise();
    for (Invoice invoice : invoices) {
      conn.update(INVOICE_TABLE, invoice, invoice.getId())
        .onFailure(e -> log.error("Error to update invoice '{}' in INVOICE_TABLE '{}'", invoice.getId(), INVOICE_TABLE));
    }
    promise.complete();
    return promise.future();
  }

}
