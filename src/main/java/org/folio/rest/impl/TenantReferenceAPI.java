package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.spring.SpringContextUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class TenantReferenceAPI extends TenantAPI {
  private static final Logger log = LogManager.getLogger(TenantReferenceAPI.class);

  private static final String PARAMETER_LOAD_SAMPLE = "loadSample";
  private static final String PARAMETER_LOAD_SYSTEM = "loadSystem";

  public TenantReferenceAPI() {
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
      .compose(v -> {

        Promise<Integer> promise = Promise.promise();

        tl.perform(attributes, headers, vertx, res -> {
          if (res.failed()) {
            log.error("Failed to load tenant data", res.cause());
            promise.fail(res.cause());
          } else {
            log.info("Tenant data loaded successfully");
            promise.complete(res.result());
          }
        });
        return promise.future();
      })
      .onFailure(throwable -> Future.failedFuture(throwable.getCause()));
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
