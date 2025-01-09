package org.folio.rest.core;

import io.vertx.core.http.HttpMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.core.models.RequestEntry;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.utils.HelperUtils.verifyAndExtractBody;

public class RestClient {

  private static final Logger log = LogManager.getLogger();
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling %s %s - %s";
  public static final String OKAPI_URL = "x-okapi-url";

  public <T> CompletableFuture<T> getById(String baseEndpoint, String id, RequestContext requestContext, Class<T> responseType) {
    RequestEntry requestEntry = new RequestEntry(baseEndpoint).withPathParameter("id", id);
    return get(requestEntry, requestContext, responseType);
  }

  public <S> CompletableFuture<S> get(RequestEntry requestEntry, RequestContext requestContext, Class<S> responseType) {
    CompletableFuture<S> future = new CompletableFuture<>();
    String endpoint = requestEntry.buildEndpoint();
    HttpClientInterface client = getHttpClient(requestContext.getHeaders());
    log.debug("Calling GET {}", endpoint);
    try {
      client
        .request(HttpMethod.GET, endpoint, requestContext.getHeaders())
        .thenApply(response -> {
          log.debug("Validating response for GET {}", endpoint);
          return verifyAndExtractBody(response);
        })
        .thenAccept(body -> {
          client.closeClient();
          S responseEntity = body.mapTo(responseType);
          future.complete(responseEntity);
        })
        .exceptionally(t -> {
          client.closeClient();
          log.error(String.format(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint, requestContext), t);
          future.completeExceptionally(t.getCause());
          return null;
        });
    } catch (Exception e) {
      log.error(String.format(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, requestEntry.getBaseEndpoint(), requestContext), e);
      client.closeClient();
      future.completeExceptionally(e);
    }
    return future;
  }

  protected HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = okapiHeaders.getOrDefault(RestClient.OKAPI_URL, "");
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }
}
