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
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
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
  private EventLoopContext ctxMock;
  @Mock
  private WebClient webClient;

  private Map<String, String> okapiHeaders;
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks(){
    MockitoAnnotations.openMocks(this);
    okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + 8081);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(ctxMock, okapiHeaders);

    // Mock Vertx and Context behavior
    VertxInternal vertx = mock(VertxInternal.class);
    when(ctxMock.owner()).thenReturn(vertx);
  }

  @Test
  void testGetShouldReturnJsonObject() {
    RestClient restClient = Mockito.spy(new RestClient());
    String uuid = UUID.randomUUID().toString();
    String endpoint = "/invoice/" + uuid; // Adjusted for clarity
    Invoice expTransaction = new Invoice().withId(uuid);
    JsonObject expectedResponse = JsonObject.mapFrom(expTransaction);


    // Mock the WebClient
    WebClient mockWebClient = mock(WebClient.class);
    Mockito.doReturn(mockWebClient).when(restClient).getVertxWebClient(any());

    // Mock the HttpRequest
    HttpRequest<Buffer> mockHttpRequest = mock(HttpRequest.class);
    Mockito.when(mockWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);

    // Ensure the fluent API pattern is maintained by returning the mockHttpRequest itself
    Mockito.when(mockHttpRequest.putHeaders(any())).thenReturn(mockHttpRequest);
    Mockito.when(mockHttpRequest.expect(any(ResponsePredicate.class))).thenReturn(mockHttpRequest);

    // Mock the HttpResponse
    HttpResponse<JsonObject> mockHttpResponse = mock(HttpResponse.class);
    Mockito.when(mockHttpResponse.bodyAsJsonObject()).thenReturn(expectedResponse);

    // Mock the asynchronous behavior using thenAnswer
    Mockito.when(mockHttpRequest.send()).thenAnswer(invocation -> Future.succeededFuture(mockHttpResponse));

    // Execute the test
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






