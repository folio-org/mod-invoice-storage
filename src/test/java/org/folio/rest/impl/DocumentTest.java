package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;

import org.folio.rest.jaxrs.model.Document;
import org.folio.rest.jaxrs.model.DocumentCollection;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.jupiter.api.Test;

public class DocumentTest extends TestBase {
  private static final String SAMPLE_INVOICE_FILE = "data/invoices/12345_paid.json";
  private static final String SAMPLE_DOCUMENT_FILE = "data/documents/doc_for_invoice_6b8bc989.json";
  private static final String SAMPLE_DOCUMENT_FILE_2 = "data/documents/doc_for_invoice_07bb89be.json";
  private static final String INVOICE_ID = "6b8bc989-834d-4a14-945b-4c5442ae09af";
  private static final String DOCUMENT_ENDPOINT = "/invoice-storage/invoices/" + INVOICE_ID + "/documents";
  private static final String DOCUMENT_ENDPOINT_WITH_ID = DOCUMENT_ENDPOINT + "/{id}";
  private static final String DOCUMENT_ID = "433f8140-001e-4605-b5a8-f02793f3d2ec";

  private final Logger logger = LoggerFactory.getLogger(DocumentTest.class);

  @Test
  public void testDocumentsCrud() throws MalformedURLException {
    try {
      logger.info("--- mod-invoice-storage Document test:");

      // prepare invoice
      String invoiceSample = getFile(SAMPLE_INVOICE_FILE);
      postData(INVOICE.getEndpoint(), invoiceSample).then().statusCode(201);

      String sampleDocument = getFile(SAMPLE_DOCUMENT_FILE);
      postData(DOCUMENT_ENDPOINT, sampleDocument).then()
        .statusCode(201)
        .extract()
        .response()
        .as(Document.class);

      logger.info("--- mod-invoice-storage Document test: Try to create document with mismatched id");
      postData(DOCUMENT_ENDPOINT, getFile(SAMPLE_DOCUMENT_FILE_2)).then().statusCode(500);

      logger.info(String.format("--- mod-invoice-storage  test: Fetching with ID: %s", INVOICE_ID));
      Document createdDocument = getDataById(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID).then()
        .log().ifValidationFails()
        .statusCode(200).log().ifValidationFails()
        .extract()
        .body().as(Document.class);

      assertEquals(INVOICE_ID, createdDocument.getInvoiceId());

      DocumentCollection documents = getData(DOCUMENT_ENDPOINT)
        .then()
        .log().ifValidationFails()
        .statusCode(200).log().ifValidationFails()
        .extract()
        .body().as(DocumentCollection.class);

      // check base64 content not present in documents list
      boolean base64Exists = documents.getDocuments()
        .stream()
        .anyMatch(document -> document.getContents() != null);
      assertFalse(base64Exists);

      // test edit document
      putData(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID, sampleDocument).then()
        .log()
        .ifValidationFails()
        .statusCode(501);

    } catch (Exception e) {
      logger.error(String.format("--- mod-invoice-storage-test:  API ERROR: %s", e.getMessage()));
      fail(e.getMessage());
    } finally {
      logger.info(String.format("--- mod-invoice-storage test: Deleting document with ID: %s", DOCUMENT_ID));
      deleteDataSuccess(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID);
      testVerifyEntityDeletion(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID);

      logger.info(String.format("--- mod-invoice-storage test: Verify invoice is deleted by ID: %s", INVOICE_ID));

      deleteDataSuccess(INVOICE.getEndpointWithId(), INVOICE_ID);
      testVerifyEntityDeletion(INVOICE.getEndpointWithId(), INVOICE_ID);
    }
  }

  @Test
  public void testFetchEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage get document by id test: Invalid id: %s", NON_EXISTED_ID));
    getDataById(DOCUMENT_ENDPOINT_WITH_ID, NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @Test
  public void testDeleteEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage delete document by id test: Invalid id: %s", NON_EXISTED_ID));
    deleteData(DOCUMENT_ENDPOINT_WITH_ID, NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }
}
