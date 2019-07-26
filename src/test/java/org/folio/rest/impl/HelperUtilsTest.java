package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;

import java.net.URL;

import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.junit.jupiter.api.Test;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import mockit.Mock;
import mockit.MockUp;

class HelperUtilsTest extends TestBase {

  private static final String DOCUMENTS_ENDPOINT = "/invoice-storage/invoices/6b8bc989-834d-4a14-945b-4c5442ae09af/documents";
  private static final String DOCUMENT_ID = "433f8140-001e-4605-b5a8-f02793f3d2ec";

  private static RequestSpecification rs = given().header(TENANT_HEADER)
    .header(USER_ID_HEADER)
    .header(X_OKAPI_TOKEN);

  @Test
  void getDocumentsPgClientRuntimeException() throws Exception {
    new MockUp<PostgresClient>() {
      @Mock
      PostgresClient get(String table, Class<Document> clazz, Criterion filter, boolean returnCount,
          Handler<AsyncResult<Results<Document>>> replyHandler) {
        replyHandler.handle(Future.failedFuture(new RuntimeException()));
        return null;
      }
    };
    get(storageUrl(DOCUMENTS_ENDPOINT)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);

    get(storageUrl(DOCUMENTS_ENDPOINT + "/" + DOCUMENT_ID)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);
  }

  private ValidatableResponse get(URL endpoint) {
    return rs.get(endpoint).then();
  }
}
