package org.folio.rest.impl;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.VoucherLine;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.folio.rest.utils.TestEntities.VOUCHER;
import static org.folio.rest.utils.TestEntities.VOUCHER_LINES;

public class ForeignKeysTest extends TestBase {

  private static final String ID_DOES_NOT_EXIST = "bad500bb-bbbb-500b-bbbb-bbbbbbbbbbbb";
  private static final String EXISTENT_VOUCHER_ID = "a9b99f8a-7100-47f2-9903-6293d44a9905";

  @Test
  public void testAddVoucherLineReferencedToNonExistentVoucher() throws MalformedURLException {
    VoucherLine voucherLine = new JsonObject(getFile(VOUCHER_LINES.getSampleFileName())).mapTo(VoucherLine.class);
    voucherLine.setVoucherId(ID_DOES_NOT_EXIST);

    postData(VOUCHER_LINES.getEndpoint(), JsonObject.mapFrom(voucherLine).encodePrettily()).then().statusCode(400);
  }

  @Test
  public void testDeleteVoucherThatVoucherLinesReferencedTo() throws MalformedURLException {
    String voucherLine = getFile(VOUCHER_LINES.getSampleFileName());
    String sampleId = postData(VOUCHER_LINES.getEndpoint(), voucherLine).then().statusCode(201).extract().path("id");

    deleteData(VOUCHER.getEndpointWithId(), EXISTENT_VOUCHER_ID).then().statusCode(500);

    deleteData(VOUCHER_LINES.getEndpointWithId(), sampleId).then().statusCode(204);
  }

  @Test
  public void testUpdateVoucherLineSoItRefersToNonExistentVoucher() throws MalformedURLException {
    String voucherLine = getFile(VOUCHER_LINES.getSampleFileName());
    String sampleId = postData(VOUCHER_LINES.getEndpoint(), voucherLine).then().statusCode(201).extract().path("id");

    VoucherLine updatedLine = new JsonObject(voucherLine).mapTo(VoucherLine.class);
    updatedLine.setVoucherId(ID_DOES_NOT_EXIST);

    putData(VOUCHER_LINES.getEndpointWithId(), sampleId, JsonObject.mapFrom(updatedLine).encodePrettily()).then().statusCode(400);
    deleteData(VOUCHER_LINES.getEndpointWithId(), sampleId).then().statusCode(204);
  }
}
