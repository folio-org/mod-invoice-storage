package org.folio.rest.impl;

import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.DocumentCollection;
import org.folio.rest.jaxrs.model.InvoiceDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DocumentTest extends TestBase {
  private static final String SAMPLE_INVOICE_FILE = "data/invoices/12345_paid.json";
  private static final String SAMPLE_INVOICE_FILE_2 = "data/invoices/45723_canceled.json";

  private static final String SAMPLE_DOCUMENT_FILE = "data/documents/doc_for_invoice_6b8bc989.json";
  private static final String SAMPLE_DOCUMENT_FILE_2 = "data/documents/doc_for_invoice_07bb89be.json";

  private static final String SAMPLE_DOCUMENT_INVALID_INVOICE = "data/documents/doc_for_invoice_733cafd3.json";

  private static final String INVOICE_ID = "6b8bc989-834d-4a14-945b-4c5442ae09af";
  private static final String ANOTHER_INVOICE_ID = "07bb89be-dd4a-42f8-bbd0-a648e14bac5d";

  private static final String DOCUMENT_ENDPOINT = "/invoice-storage/invoices/" + INVOICE_ID + "/documents";
  private static final String ANOTHER_DOCUMENT_ENDPOINT = "/invoice-storage/invoices/" + ANOTHER_INVOICE_ID + "/documents";

  private static final String DOCUMENT_ENDPOINT_WITH_ID = DOCUMENT_ENDPOINT + "/{id}";
  private static final String ANOTHER_DOCUMENT_ENDPOINT_WITH_ID = ANOTHER_DOCUMENT_ENDPOINT + "/{id}";
  private static final String DOCUMENT_ID = "433f8140-001e-4605-b5a8-f02793f3d2ec";
  private static final String ANOTHER_DOCUMENT_ID = "1f6c1af3-6475-43a2-8626-e2496616601c";

  private static final Logger LOGGER = LogManager.getLogger(DocumentTest.class);

  @Test
  void testDocumentsCrud() throws MalformedURLException {
    try {
      LOGGER.info("--- mod-invoice-storage Document test:");

      LOGGER.info("--- mod-invoice-storage Document test: prepare two invoices");
      postData(INVOICE.getEndpoint(), getFile(SAMPLE_INVOICE_FILE)).then()
        .statusCode(201);
      postData(INVOICE.getEndpoint(), getFile(SAMPLE_INVOICE_FILE_2)).then()
        .statusCode(201);

      LOGGER.info("--- mod-invoice-storage Document test: prepare two create two documents");
      postData(DOCUMENT_ENDPOINT, getFile(SAMPLE_DOCUMENT_FILE)).then()
        .statusCode(201);
      postData(ANOTHER_DOCUMENT_ENDPOINT, getFile(SAMPLE_DOCUMENT_FILE_2)).then()
        .statusCode(201);

      LOGGER.info("--- mod-invoice-storage Document test: Try to create document with mismatched invoiceId");
      postData(DOCUMENT_ENDPOINT, getFile(SAMPLE_DOCUMENT_INVALID_INVOICE)).then()
        .statusCode(400);

      LOGGER.info(String.format("--- mod-invoice-storage  test: Fetching with ID: %s", INVOICE_ID));
      InvoiceDocument createdDocument = getDataById(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID).then()
        .log().ifValidationFails()
        .statusCode(200)
        .log().ifValidationFails()
        .extract()
        .body()
        .as(InvoiceDocument.class);

      assertEquals(INVOICE_ID, createdDocument.getDocumentMetadata().getInvoiceId());
      assertNotNull(createdDocument.getContents().getData());

      LOGGER.info(String.format("--- mod-invoice-storage  test: Get list of documents for invoice %s", INVOICE_ID));
      DocumentCollection documents = getData(DOCUMENT_ENDPOINT).then()
        .log().ifValidationFails()
        .statusCode(200)
        .log().ifValidationFails()
        .extract()
        .body()
        .as(DocumentCollection.class);
      // check only one document was found
      Assertions.assertEquals(1, documents.getTotalRecords());

      LOGGER.info(String.format("--- mod-invoice-storage  test: Get list of documents by query for invoice %s", INVOICE_ID));
      DocumentCollection documentsByQuery = getData(DOCUMENT_ENDPOINT + "?query=name==sample.pdf OR name<>sample.pdf sortBy name").then()
        .log().ifValidationFails()
        .statusCode(200)
        .log().ifValidationFails()
        .extract()
        .body()
        .as(DocumentCollection.class);
      // check only one document was found
      Assertions.assertEquals(1, documentsByQuery.getTotalRecords());

    } catch (Exception e) {
      LOGGER.error(String.format("--- mod-invoice-storage-test:  API ERROR: %s", e.getMessage()));
      fail(e.getMessage());
    } finally {
      LOGGER.info(String.format("--- mod-invoice-storage test: Deleting document with ID: %s", DOCUMENT_ID));
      deleteDataSuccess(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID);
      deleteDataSuccess(ANOTHER_DOCUMENT_ENDPOINT_WITH_ID, ANOTHER_DOCUMENT_ID);
      testVerifyEntityDeletion(DOCUMENT_ENDPOINT_WITH_ID, DOCUMENT_ID);
      testVerifyEntityDeletion(ANOTHER_DOCUMENT_ENDPOINT_WITH_ID, ANOTHER_DOCUMENT_ID);

      LOGGER.info(String.format("--- mod-invoice-storage test: Verify invoice is deleted by ID: %s", INVOICE_ID));
      deleteDataSuccess(INVOICE.getEndpointWithId(), INVOICE_ID);
      testVerifyEntityDeletion(INVOICE.getEndpointWithId(), INVOICE_ID);
      deleteDataSuccess(INVOICE.getEndpointWithId(), ANOTHER_INVOICE_ID);
      testVerifyEntityDeletion(INVOICE.getEndpointWithId(), ANOTHER_INVOICE_ID);
    }
  }

  @Test
  void testFetchEntityWithNonExistedId() throws MalformedURLException {
    LOGGER.info(String.format("--- mod-invoice-storage get document by id test: Invalid id: %s", NON_EXISTED_ID));
    getDataById(DOCUMENT_ENDPOINT_WITH_ID, NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }

  @Test
  void testDeleteEntityWithNonExistedId() throws MalformedURLException {
    LOGGER.info(String.format("--- mod-invoice-storage delete document by id test: Invalid id: %s", NON_EXISTED_ID));
    deleteData(DOCUMENT_ENDPOINT_WITH_ID, NON_EXISTED_ID).then()
      .log()
      .ifValidationFails()
      .statusCode(404);
  }
}
