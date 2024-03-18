package org.folio.rest.core;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.utils.RestConstants.OKAPI_URL;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.WebClientFactory;
import org.folio.rest.core.models.RequestContext;

public class RestClient {

  private static final Logger log = LogManager.getLogger(RestClient.class);
  public static final String REQUEST_MESSAGE_LOG_INFO = "Calling {} {}";
  private static final String EXCEPTION_CALLING_ENDPOINT_MSG = "Exception calling {} {} {}";
  private final Vertx vertx = Vertx.currentContext() == null ? Vertx.vertx() : Vertx.currentContext().owner();
  private final WebClient webClient = WebClientFactory.getWebClient(vertx);

  public Future<JsonObject> get(String endpoint, RequestContext requestContext) {
    log.info(REQUEST_MESSAGE_LOG_INFO, HttpMethod.GET, endpoint);
    var caseInsensitiveHeader = convertToCaseInsensitiveMap(requestContext.getHeaders());
    var absEndpoint = buildAbsEndpoint(caseInsensitiveHeader, endpoint);

    return webClient.getAbs(absEndpoint)
      .putHeaders(caseInsensitiveHeader)
      .expect(ResponsePredicate.SC_OK)
      .send()
      .map(HttpResponse::bodyAsJsonObject)
      .onFailure(e -> log.error(EXCEPTION_CALLING_ENDPOINT_MSG, HttpMethod.GET, endpoint, e.getMessage()));
  }

  protected String buildAbsEndpoint(MultiMap okapiHeaders, String endpoint) {
    var okapiURL = okapiHeaders.get(OKAPI_URL);
    return okapiURL + endpoint;
  }

  protected MultiMap convertToCaseInsensitiveMap(Map<String, String> okapiHeaders) {
    return MultiMap.caseInsensitiveMultiMap()
      .addAll(okapiHeaders)
      // set default Accept header
      .add("Accept", APPLICATION_JSON + ", " + TEXT_PLAIN);
  }
}
