package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherLine;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.folio.rest.utils.TestEntities.INVOICE;
import static org.folio.rest.utils.TestEntities.INVOICE_LINES;
import static org.folio.rest.utils.TestEntities.VOUCHER;
import static org.folio.rest.utils.TestEntities.VOUCHER_LINES;

public class ForeignKeysTest extends TestBase {

  @Test
  public void testCreateVoucherLineReferencedToNonExistentVoucher() throws MalformedURLException {
    VoucherLine voucherLine = new JsonObject(getFile(VOUCHER_LINES.getSampleFileName())).mapTo(VoucherLine.class);
    voucherLine.setVoucherId(NON_EXISTED_ID);

    postData(VOUCHER_LINES.getEndpoint(), JsonObject.mapFrom(voucherLine).encodePrettily()).then().statusCode(400);
  }

  @Test
  public void testDeleteVoucherThatVoucherLinesReferencedTo() throws MalformedURLException {
    String voucherLine = getFile(VOUCHER_LINES.getSampleFileName());
    String sampleId = postData(VOUCHER_LINES.getEndpoint(), voucherLine).then().statusCode(201).extract().path(ID);

    deleteData(VOUCHER.getEndpointWithId(), StorageTestSuite.EXISTENT_VOUCHER_ID).then().statusCode(500);

    deleteData(VOUCHER_LINES.getEndpointWithId(), sampleId).then().statusCode(204);
  }

  @Test
  public void testUpdateVoucherLineSoItRefersToNonExistentVoucher() throws MalformedURLException {
    String voucherLine = getFile(VOUCHER_LINES.getSampleFileName());
    String sampleId = postData(VOUCHER_LINES.getEndpoint(), voucherLine).then().statusCode(201).extract().path(ID);

    VoucherLine updatedLine = new JsonObject(voucherLine).mapTo(VoucherLine.class);
    updatedLine.setVoucherId(NON_EXISTED_ID);

    putData(VOUCHER_LINES.getEndpointWithId(), sampleId, JsonObject.mapFrom(updatedLine).encodePrettily()).then().statusCode(400);
    deleteData(VOUCHER_LINES.getEndpointWithId(), sampleId).then().statusCode(204);
  }

  @Test
  public void testCreateInvoiceLineReferencedToNonExistentInvoice() throws MalformedURLException {
    InvoiceLine invoiceLine = new JsonObject(getFile(INVOICE_LINES.getSampleFileName())).mapTo(InvoiceLine.class);
    invoiceLine.setInvoiceId(NON_EXISTED_ID);

    postData(INVOICE_LINES.getEndpoint(), JsonObject.mapFrom(invoiceLine).encodePrettily()).then().statusCode(400);
  }

  @Test
  public void testCreateVoucherReferencedToNonExistentInvoice() throws MalformedURLException {
    Voucher voucher = new JsonObject(getFile(VOUCHER.getSampleFileName())).mapTo(Voucher.class);
    voucher.setInvoiceId(NON_EXISTED_ID);

    postData(VOUCHER.getEndpoint(), JsonObject.mapFrom(voucher).encodePrettily()).then().statusCode(400);
  }

  @Test
  public void testDeleteInvoiceThatVoucherReferencedTo() throws MalformedURLException {

    deleteData(INVOICE.getEndpointWithId(), StorageTestSuite.EXISTENT_INVOICE_ID).then().statusCode(500);
  }

  @Test
  public void testUpdateInvoiceLineSoItRefersToNonExistentInvoice() throws MalformedURLException {
    String invoiceLine = getFile(INVOICE_LINES.getSampleFileName());
    String sampleId = postData(INVOICE_LINES.getEndpoint(), invoiceLine).then().statusCode(201).extract().path(ID);

    InvoiceLine updatedLine = new JsonObject(invoiceLine).mapTo(InvoiceLine.class);
    updatedLine.setInvoiceId(NON_EXISTED_ID);

    putData(INVOICE_LINES.getEndpointWithId(), sampleId, JsonObject.mapFrom(updatedLine).encodePrettily()).then().statusCode(400);
    deleteData(INVOICE_LINES.getEndpointWithId(), sampleId).then().statusCode(204);
  }

  @Test
  public void testUpdateVoucherSoItRefersToNonExistentInvoice() throws MalformedURLException {
    Voucher voucher = getDataById(VOUCHER.getEndpointWithId(), StorageTestSuite.EXISTENT_VOUCHER_ID).then().statusCode(200).extract().body().as(Voucher.class);
    voucher.setInvoiceId(NON_EXISTED_ID);

    putData(VOUCHER.getEndpointWithId(), StorageTestSuite.EXISTENT_VOUCHER_ID, JsonObject.mapFrom(voucher).encodePrettily()).then().statusCode(400);
  }
}
