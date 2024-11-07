package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;

public class AuditOutboxAPITest extends TestBase {

  public static final String AUDIT_OUTBOX_ENDPOINT = "/invoice-storage/audit-outbox/process";

  @Test
  void testPostInvoiceStorageAuditOutboxProcess() throws MalformedURLException {
    given()
      .spec(commonRequestSpec())
      .when()
      .post(storageUrl(AUDIT_OUTBOX_ENDPOINT))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .response();
  }

}
