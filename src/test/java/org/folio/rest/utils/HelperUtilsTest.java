package org.folio.rest.utils;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.folio.rest.impl.TestBase.TENANT_HEADER;
import static org.folio.rest.impl.TestBase.USER_ID_HEADER;
import static org.folio.rest.impl.TestBase.X_OKAPI_TOKEN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.util.concurrent.CompletionException;

import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.tools.client.Response;
import org.junit.jupiter.api.Test;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import mockit.Mock;
import mockit.MockUp;

public class HelperUtilsTest {

  private static final String DOCUMENTS_ENDPOINT = "/invoice-storage/invoices/6b8bc989-834d-4a14-945b-4c5442ae09af/documents";
  private static final String DOCUMENT_ID = "433f8140-001e-4605-b5a8-f02793f3d2ec";

  private static final RequestSpecification rs = given().header(TENANT_HEADER)
    .header(USER_ID_HEADER)
    .header(X_OKAPI_TOKEN);

  @Test
  void testShouldEncodeQuery() {
    assertThat(HelperUtils.encodeQuery("?limit=123&offset=0"), is("%3Flimit%3D123%26offset%3D0"));
  }

  @Test
  void testShouldThrowCompletionExceptionForSpecificCodeRange() {
    Response response = new Response();
    response.setCode(100);
    JsonObject error = new JsonObject();
    response.setError(error);
    assertThrows(CompletionException.class, () -> {
      HelperUtils.verifyResponse(response);
    });
  }

  @Test
  void getDocumentByIdPgClientRuntimeException() throws Exception {
    new MockUp<PostgresClient>() {
      @Mock
      PostgresClient select(String query, Handler<AsyncResult<Results<Document>>> replyHandler) {
        replyHandler.handle(Future.failedFuture(new RuntimeException()));
        return null;
      }
    };

    get(storageUrl(DOCUMENTS_ENDPOINT + "/" + DOCUMENT_ID)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);
  }

  private ValidatableResponse get(URL endpoint) {
    return rs.get(endpoint).then();
  }
}
