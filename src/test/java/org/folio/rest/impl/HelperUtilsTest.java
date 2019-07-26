package org.folio.rest.impl;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.Context;
import mockit.Mock;
import mockit.MockUp;
import org.folio.HttpStatus;
import org.folio.rest.persist.EntitiesMetadataHolder;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLQueryValidationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;

public class HelperUtilsTest extends TestBase {

  private static final String VOUCHERS_ENDPOINT = "/voucher-storage/vouchers";

  @Test
  public void getEntitiesCollectionWithDistinctOnFailCqlExTest() throws Exception {
    new MockUp<PgUtil>()
    {
      @Mock
      PostgresClient postgresClient(Context vertxContext, Map<String, String> okapiHeaders) {
        throw new CQLQueryValidationException(null);
      }
    };
    get(storageUrl(VOUCHERS_ENDPOINT)).statusCode(HttpStatus.HTTP_BAD_REQUEST.toInt()).contentType(TEXT_PLAIN);
  }


  @Test
  public void entitiesMetadataHolderRespond400FailTest() throws Exception {
    new MockUp<EntitiesMetadataHolder>()
    {
      @Mock
      Method getRespond400WithTextPlainMethod() throws NoSuchMethodException {
        throw new NoSuchMethodException();
      }
    };
    get(storageUrl(VOUCHERS_ENDPOINT)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);
  }

  @Test
  public void entitiesMetadataHolderRespond500FailTest() throws Exception {
    new MockUp<EntitiesMetadataHolder>()
    {
      @Mock
      Method getRespond500WithTextPlainMethod() throws NoSuchMethodException {
        throw new NoSuchMethodException();
      }
    };
    get(storageUrl(VOUCHERS_ENDPOINT)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);
  }

  @Test
  public void entitiesMetadataHolderRespond200FailTest() throws Exception {
    new MockUp<EntitiesMetadataHolder>()
    {
      @Mock
      Method getRespond200WithApplicationJson() throws NoSuchMethodException {
        throw new NoSuchMethodException();
      }
    };
    get(storageUrl(VOUCHERS_ENDPOINT)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);
  }

  @Test
  public void getEntitiesCollectionWithDistinctOnFailNpExTest() throws Exception {
    new MockUp<PgUtil>()
    {
      @Mock
      PostgresClient postgresClient(Context vertxContext, Map<String, String> okapiHeaders) {
        throw new NullPointerException();
      }
    };
    get(storageUrl(VOUCHERS_ENDPOINT)).statusCode(HttpStatus.HTTP_INTERNAL_SERVER_ERROR.toInt()).contentType(TEXT_PLAIN);
  }

  private ValidatableResponse get(URL endpoint) {
    return RestAssured
      .with()
        .header(TENANT_HEADER)
        .get(endpoint)
          .then();
  }
}
