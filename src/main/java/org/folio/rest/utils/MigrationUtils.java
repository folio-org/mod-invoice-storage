package org.folio.rest.utils;

import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;
import static org.folio.service.util.CommonServiceUtil.collectResultsOnSuccess;
import static org.folio.service.util.CommonServiceUtil.convertIdsToCqlQuery;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import one.util.streamex.StreamEx;
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
  public static final int MAX_IDS_FOR_GET_RQ = 15;

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
      .compose(invoices -> getTransactionsByInvoiceIdsAndPopulate(invoices, requestContext))
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

  private static Future<List<Invoice>> getTransactionsByInvoiceIdsAndPopulate(List<Invoice> invoices, RequestContext requestContext) {
    if (invoices.isEmpty()) {
      log.info("getTransactionsByInvoiceIds:: No invoices to process");
      return Future.succeededFuture();
    }

    // Extract the invoice IDs from the list of invoices
    List<String> invoiceIds = invoices.stream()
      .map(Invoice::getId)
      .toList();

    return retrieveTransactionsByInvoiceIds(invoiceIds, requestContext)
      .compose(response -> populateInvoicesWithFiscalYears(invoices, response));
  }

  private static Future<Void> updateInvoicesWithFiscalYearId(List<Invoice> invoices, Conn conn) {
    if (invoices.isEmpty()) {
      return Future.succeededFuture();
    }
    return conn.updateBatch(INVOICE_TABLE, invoices)
      .onFailure(e -> log.error("Error to update '{}' invoice(s) in INVOICE_TABLE '{}'", invoices, INVOICE_TABLE))
      .mapEmpty();
  }

  private static Future<List<Invoice>> populateInvoicesWithFiscalYears(List<Invoice> invoices, List<JsonObject> response) {
    Map<String, String> invoiceIdToFiscalYearMap = extractInvoiceIdToFiscalYearMap(response);
    invoices.forEach(invoice -> invoice.setFiscalYearId(invoiceIdToFiscalYearMap.get(invoice.getId())));
    log.info("populateInvoicesWithFiscalYears: Populated invoices with fiscal year IDs. invoices: {}", invoices);
    return Future.succeededFuture(invoices);
  }

  private static Map<String, String> extractInvoiceIdToFiscalYearMap(List<JsonObject> responseList) {
    log.info("extractInvoiceIdToFiscalYearMap: Extracting fiscalYearId and sourceInvoiceId from responseList: {}", responseList);
    return responseList.stream()
      .flatMap(response -> response.getJsonArray("transactions").stream())
      .map(JsonObject.class::cast)
      .collect(Collectors.toMap(
        transaction -> transaction.getString("sourceInvoiceId"),
        transaction -> transaction.getString("fiscalYearId")
      ));
  }

  public static Future<List<JsonObject>> retrieveTransactionsByInvoiceIds(List<String> invoiceIds, RequestContext requestContext) {
    List<Future<JsonObject>> futures = StreamEx.ofSubLists(invoiceIds, MAX_IDS_FOR_GET_RQ)
      .map(ids -> retrieveTransactions(convertIdsToCqlQuery(ids, "sourceInvoiceId"), requestContext))
      .collect(Collectors.toList());

    return collectResultsOnSuccess(futures);
  }

  public static Future<JsonObject> retrieveTransactions(String query, RequestContext requestContext) {
    RestClient restClient = new RestClient();
    var requestEntry = new RequestEntry(TRANSACTION_API)
      .withOffset(0)
      .withLimit(Integer.MAX_VALUE)
      .withQuery(query);
    log.info("getTransactionsByInvoiceIds:: Getting transaction by calling to finance-storage module: query={}", query);
    return restClient.get(requestEntry.buildEndpoint(), requestContext)
      .onFailure(e -> log.info("Error to get transactions with query={} and api={}", query, TRANSACTION_API));
  }
}
