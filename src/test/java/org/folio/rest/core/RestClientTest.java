package org.folio.rest.core;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.utils.TestConstants.OKAPI_URL;
import static org.folio.rest.utils.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.utils.TestConstants.X_OKAPI_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.restassured.http.Header;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RestClientTest {
  public static final Header X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, "invoiceimpltest");

  @Mock
  private Context ctxMock;
  @Mock
  private WebClient webClient;
  @Mock
  private Context contextMock;
  @Mock
  private Vertx vertxMock;

  private RequestContext requestContext;
  private Map<String, String> okapiHeaders;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.openMocks(this);

    okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + 8081);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());

    when(contextMock.owner()).thenReturn(vertxMock);
    requestContext = new RequestContext(contextMock, okapiHeaders);
  }

  @Test
  void testGetShouldReturnJsonObject() {
    RestClient restClient = Mockito.spy(new RestClient());
    String uuid = UUID.randomUUID().toString();
    String endpoint = "/invoice/" + uuid;
    Invoice expTransaction = new Invoice().withId(uuid);
    JsonObject expectedResponse = JsonObject.mapFrom(expTransaction);

    WebClient mockWebClient = mock(WebClient.class);

    HttpRequest<Buffer> mockHttpRequest = mock(HttpRequest.class);
    when(mockWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeaders(any())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.expect(any(ResponsePredicate.class))).thenReturn(mockHttpRequest);
    HttpResponse<JsonObject> mockHttpResponse = mock(HttpResponse.class);
    when(mockHttpResponse.bodyAsJsonObject()).thenReturn(expectedResponse);
    when(mockHttpRequest.send()).thenAnswer(invocation -> Future.succeededFuture(mockHttpResponse));

    restClient.get(endpoint, requestContext).onComplete(ar -> {
      if (ar.succeeded()) {
        JsonObject responseObject = ar.result();
        assertEquals(responseObject.getString("id"), expTransaction.getId());
      } else {
        fail("The request did not succeed.");
      }
    });
  }
}






