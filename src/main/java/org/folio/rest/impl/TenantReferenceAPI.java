package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;
import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_TABLE;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.ModuleId;
import org.folio.okapi.common.SemVer;
import org.folio.rest.core.RestClient;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.CriterionBuilder;
import org.folio.rest.persist.DBClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.spring.SpringContextUtil;

public class TenantReferenceAPI extends TenantAPI {
  private static final Logger log = LogManager.getLogger(TenantReferenceAPI.class);

  private static final String PARAMETER_LOAD_SAMPLE = "loadSample";
  private static final String PARAMETER_LOAD_SYSTEM = "loadSystem";

  private final RestClient restClient;

  public TenantReferenceAPI(RestClient restClient) {
    this.restClient = restClient;
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
    log.debug("Init TenantReferenceAPI");
  }

  @Override
  public Future<Integer> loadData(TenantAttributes attributes, String tenantId, Map<String, String> headers, Context vertxContext) {
    log.debug("Trying to load tenant data with tenantId: {}", tenantId);
    Vertx vertx = vertxContext.owner();
    Parameter parameter = new Parameter().withKey(PARAMETER_LOAD_SYSTEM).withValue("true");
    attributes.getParameters().add(parameter);

    TenantLoading tl = new TenantLoading();
    buildDataLoadingParameters(attributes, tl);

    return Future.succeededFuture()
      .compose(v -> migration(attributes, "mod-invoice-storage-5.7.0", () -> populateInvoiceWithFiscalYear(headers, vertxContext)))
      .compose(v -> migration(attributes, "mod-invoice-storage-5.6.0", () -> populateInvoiceWithFiscalYear(headers, vertxContext)))
      .compose(v -> {
        Promise<Integer> promise = Promise.promise();
        tl.perform(attributes, headers, vertx, res -> {
          if (res.failed()) {
            promise.fail(res.cause());
          } else {
            promise.complete();
          }
        });
        return promise.future();
      });
  }

  private Future<Void> populateInvoiceWithFiscalYear(Map<String, String> headers, Context vertxContext) {
    Promise<Void> promise = Promise.promise();
    vertxContext.runOnContext(event -> {
      DBClient client = new DBClient(vertxContext, headers);
      RequestContext requestContext = new RequestContext(vertxContext, headers);
      client.startTx()
        .compose(v -> getInvoicesWithoutFiscalYearFromDb(client))
        .compose(invoices -> getTransactionsByInvoiceIds(invoices, requestContext))
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

  private Future<List<Invoice>> getInvoicesWithoutFiscalYearFromDb(DBClient client) {
    Promise<List<Invoice>> promise = Promise.promise();
    Criteria criteria = new Criteria();
    criteria.addField("fiscalYearId");
    criteria.setOperation("IS NULL");
    Criterion criterion = new CriterionBuilder().build();
    criterion.addCriterion(criteria);

    client.getPgClient().get(INVOICE_TABLE, Invoice.class, criterion, false, reply -> {
      if (reply.failed()) {
        promise.fail("error");
      } else {
        log.info("Transaction record {} was successfully retrieved", reply.result().toString());
        List<Invoice> invoices = reply.result().getResults();
        promise.complete(invoices);
      }
    });
    return promise.future();
  }

  private Future<Void> getTransactionsByInvoiceIds(List<Invoice> invoices, RequestContext requestContext) {
    Promise<Void> promise = Promise.promise();
    RestClient restClient = new RestClient();
    // Extract the invoice IDs from the list of invoices
    List<String> invoiceIds = invoices.stream()
      .map(Invoice::getId)
      .toList();

    // Build a query string using the 'IN' operator
    String query = "invoiceId=" + "(" + String.join(",", invoiceIds.stream().map(id -> "'" + id + "'").toList()) + ")";
    RequestEntry requestEntry = new RequestEntry("/finance-storage/transactions")
      .withOffset(0)
      .withQuery(query)
      .withLimit(Integer.MAX_VALUE);

    restClient.get(requestEntry, requestContext, JsonObject.class)
      .thenApply(response -> {
      JsonArray transactions = response.getJsonArray("transactions");
      Map<String, String> invoiceIdToFiscalYearMap = new HashMap<>();

      // Iterate over the transactions and map invoice IDs to fiscal year IDs
      for (int i = 0; i < transactions.size(); i++) {
        JsonObject transaction = transactions.getJsonObject(i);
        String invoiceId = transaction.getString("invoiceId");
        String fiscalYearId = transaction.getString("fiscalYearId");
        invoiceIdToFiscalYearMap.put(invoiceId, fiscalYearId);
      }

      // Update the invoices with the fiscal year IDs
      invoices.forEach(invoice -> {
        String fiscalYearId = invoiceIdToFiscalYearMap.get(invoice.getId());
        invoice.setFiscalYearId(fiscalYearId); // Assuming Invoice has a setFiscalYearId method
      });

      promise.complete();
      return Future.succeededFuture();
      })
      .exceptionally(e -> {
        promise.fail(e.getCause());
        return Future.failedFuture(e);
      });

    return promise.future();
  }

  private Future<Void> migration(TenantAttributes attributes, String migrationModule, Supplier<Future<Void>> supplier) {
    SemVer moduleTo = moduleVersionToSemVer(migrationModule);
    SemVer currentModuleVersion = moduleVersionToSemVer(attributes.getModuleFrom());
    if (moduleTo.compareTo(currentModuleVersion) > 0) {
      return supplier.get();
    }
    return Future.succeededFuture();
  }

  private static SemVer moduleVersionToSemVer(String version) {
    try {
      return new SemVer(version);
    } catch (IllegalArgumentException ex) {
      return new ModuleId(version).getSemVer();
    }
  }

  private void buildDataLoadingParameters(TenantAttributes tenantAttributes, TenantLoading tl) {
    if (isLoadSample(tenantAttributes)) {
      tl.withKey(PARAMETER_LOAD_SAMPLE)
        .withLead("data")
        .add("batch-groups","batch-group-storage/batch-groups");
    }
  }

  private boolean isLoadSample(TenantAttributes tenantAttributes) {
    // if a system parameter is passed from command line, ex: loadSample=true
    // that value is considered,Priority of Parameters:
    // Tenant Attributes > command line parameter > default(false)
    boolean loadSample = Boolean.parseBoolean(MODULE_SPECIFIC_ARGS.getOrDefault(PARAMETER_LOAD_SAMPLE,
      "false"));
    List<Parameter> parameters = tenantAttributes.getParameters();
    for (Parameter parameter : parameters) {
      if (PARAMETER_LOAD_SAMPLE.equals(parameter.getKey())) {
        loadSample = Boolean.parseBoolean(parameter.getValue());
      }
    }
    return loadSample;
  }

  @Override
  public void deleteTenantByOperationId(String operationId, Map<String, String> headers, Handler<AsyncResult<Response>> handler,
      Context ctx) {
    log.info("Trying to delete tenant by operation id: {}", operationId);
    super.deleteTenantByOperationId(operationId, headers, res -> {
      Vertx vertx = ctx.owner();
      String tenantId = TenantTool.tenantId(headers);
      PostgresClient.getInstance(vertx, tenantId).closeClient(event -> handler.handle(res));
    }, ctx);
  }
}
