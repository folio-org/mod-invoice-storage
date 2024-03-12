package org.folio.rest.core;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.core.RestClient.OKAPI_URL;
import static org.folio.rest.utils.TestConstants.X_OKAPI_TOKEN;
import static org.folio.rest.utils.TestConstants.X_OKAPI_USER_ID;
import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.vertx.core.Context;
import org.folio.rest.core.models.RequestContext;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.restassured.http.Header;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

public class RestClientTest {
  public static final Header X_OKAPI_TENANT = new Header(OKAPI_HEADER_TENANT, "invoiceimpltest");

  @Mock
  private Context ctxMock;
  @Mock
  private HttpClientInterface httpClient;

  private Map<String, String> okapiHeaders;
  private RequestContext requestContext;

  @BeforeEach
  public void initMocks() throws Exception {
    MockitoAnnotations.openMocks(this).close();
    okapiHeaders = new HashMap<>();
    okapiHeaders.put(OKAPI_URL, "http://localhost:" + 8081);
    okapiHeaders.put(X_OKAPI_TOKEN.getName(), X_OKAPI_TOKEN.getValue());
    okapiHeaders.put(X_OKAPI_TENANT.getName(), X_OKAPI_TENANT.getValue());
    okapiHeaders.put(X_OKAPI_USER_ID.getName(), X_OKAPI_USER_ID.getValue());
    requestContext = new RequestContext(ctxMock, okapiHeaders);
  }

  @Test
  void testGetShouldSearchById() throws Exception {
    RestClient restClient = Mockito.spy(new RestClient());
    String uuid = UUID.randomUUID().toString();
    String endpoint = INVOICE.getEndpoint() + "/{id}";
    Invoice expTransaction = new Invoice().withId(uuid);
    Response response = new Response();
    response.setBody(JsonObject.mapFrom(expTransaction));
    response.setCode(200);

    doReturn(httpClient).when(restClient).getHttpClient(okapiHeaders);
    doReturn(completedFuture(response)).when(httpClient).request(eq(HttpMethod.GET), anyString(), eq(okapiHeaders));

    Invoice actInvoice = restClient.getById(endpoint, uuid, requestContext, Invoice.class).join();

    assertThat(actInvoice, equalTo(expTransaction));
  }

  @Test
  void testGetShouldThrowExceptionWhenSearchById() {
    RestClient restClient = Mockito.spy(new RestClient());
    String uuid = UUID.randomUUID().toString();
    doReturn(httpClient).when(restClient).getHttpClient(okapiHeaders);
    CompletableFuture<Invoice> result = restClient.getById(INVOICE.getEndpointWithId(), uuid, requestContext, Invoice.class);
    assertThrows(CompletionException.class, result::join);
  }

  @Test
  void testGetShouldThrowExceptionWhenSearchByIdAndResponseCodeIsNot200() throws Exception {
    RestClient restClient = Mockito.spy(new RestClient());
    String uuid = UUID.randomUUID().toString();
    String endpoint = INVOICE.getEndpoint() + "/{id}";
    Response response = new Response();
    response.setCode(404);

    doReturn(httpClient).when(restClient).getHttpClient(okapiHeaders);
    doReturn(completedFuture(response)).when(httpClient).request(eq(HttpMethod.GET), anyString(), eq(okapiHeaders));

    CompletableFuture<Invoice> result = restClient.getById(endpoint, uuid, requestContext, Invoice.class);
    assertThrows(CompletionException.class, result::join);
  }

  @Test
  void testShouldReturnHttpClient() {
    RestClient restClient = Mockito.spy(new RestClient());
    HashMap<String, String> headers = new HashMap<>();
    headers.put("x-okapi-url", "test");
    HttpClientInterface httpClient = restClient.getHttpClient(headers);
    assertThat(httpClient, notNullValue());
  }
}


