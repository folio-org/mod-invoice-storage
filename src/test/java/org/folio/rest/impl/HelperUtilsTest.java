package org.folio.rest.impl;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;

import java.net.URL;

import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.sql.SQLConnection;
import mockit.Mock;
import mockit.MockUp;

class HelperUtilsTest extends TestBase {

  private static final String DOCUMENT_ENDPOINT = "/invoice-storage/invoices/6b8bc989-834d-4a14-945b-4c5442ae09af/documents";
  private static RequestSpecification rs = RestAssured.with()
    .header(TENANT_HEADER)
    .header(USER_ID_HEADER)
    .header(X_OKAPI_TOKEN);

  @Test
  void getEntitiesNullPointerException() throws Exception {
    new MockUp<PostgresClient>() {
      @Mock
      PostgresClient get(String table, Class<Document> clazz, Criterion filter, boolean returnCount,
          Handler<AsyncResult<Results<Document>>> replyHandler) {
        replyHandler.handle(Future.failedFuture(new NullPointerException()));
        return null;
      }
    };
    get(storageUrl(DOCUMENT_ENDPOINT)).statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
      .contentType(TEXT_PLAIN);
  }

  @Test
  void postDocumentFailedSqlConnection() throws Exception {
    new MockUp<PostgresClient>() {
      @Mock
      PostgresClient getConnection(Handler<AsyncResult<SQLConnection>> replyHandler) {
        replyHandler.handle(Future.failedFuture(new NullPointerException()));
        return null;
      }
    };

    post(storageUrl(DOCUMENT_ENDPOINT)).statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt())
      .contentType(TEXT_PLAIN);
  }

  private ValidatableResponse get(URL endpoint) {
    return rs.get(endpoint)
      .then();
  }

  private ValidatableResponse post(URL endpoint) {
    return rs.post(endpoint)
      .then();
  }
}
