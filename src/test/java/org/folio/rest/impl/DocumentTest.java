package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;

import org.folio.rest.jaxrs.model.DocumentCollection;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class DocumentTest extends TestBase {
  private static final String SAMPLE_INVOICE_FILE = "data/invoices/12345_paid.json";
  private static final String SAMPLE_DOCUMENT_FILE = "data/documents/doc_for_invoice_6b8bc989.json";
  private static final String SAMPLE_DOCUMENT_FILE_2 = "data/documents/doc_for_invoice_07bb89be.json";
  private static final String INVOICE_ID = "6b8bc989-834d-4a14-945b-4c5442ae09af";
  private static final String DOCUMENT_ENDPOINT = "/invoice-storage/invoices/" + INVOICE_ID + "/documents";
  private static final String DOCUMENT_ENDPOINT_WITH_ID = DOCUMENT_ENDPOINT + "/{id}";
  private static final String DOCUMENT_ID = "433f8140-001e-4605-b5a8-f02793f3d2ec";

  private final Logger logger = LoggerFactory.getLogger(DocumentTest.class);

  @Test
  void testDocumentsCrud() throws MalformedURLException {
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
        .as(InvoiceDocument.class);

      logger.info("--- mod-invoice-storage Document test: Try to create document with mismatched id");
      postData(DOCUMENT_ENDPOINT, getFile(SAMPLE_DOCUMENT_FILE_2)).then().statusCode(400);

      logger.info(String.format("--- mod-invoice-storage  test: Fetching with ID: %s", INVOICE_ID));
      InvoiceDocument createdDocument = getDataById(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID).then()
        .log().ifValidationFails()
        .statusCode(200).log().ifValidationFails()
        .extract()
        .body().as(InvoiceDocument.class);

      assertEquals(INVOICE_ID, createdDocument.getDocumentMetadata().getInvoiceId());
      assertNotNull(createdDocument.getContents().getData());

      DocumentCollection documents = getData(DOCUMENT_ENDPOINT)
        .then()
        .log().ifValidationFails()
        .statusCode(200).log().ifValidationFails()
        .extract()
        .body().as(DocumentCollection.class);
      Assertions.assertTrue(documents.getTotalRecords() > 0);

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
  void testFetchEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage get document by id test: Invalid id: %s", NON_EXISTED_ID));
    getDataById(DOCUMENT_ENDPOINT_WITH_ID, NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @Test
  void testDeleteEntityWithNonExistedId() throws MalformedURLException {
    logger.info(String.format("--- mod-invoice-storage delete document by id test: Invalid id: %s", NON_EXISTED_ID));
    deleteData(DOCUMENT_ENDPOINT_WITH_ID, NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }
}
