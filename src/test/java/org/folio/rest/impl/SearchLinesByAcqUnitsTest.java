package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.folio.rest.utils.TestEntities.INVOICE_LINES;
import static org.folio.rest.utils.TestEntities.VOUCHERS;
import static org.folio.rest.utils.TestEntities.VOUCHER_LINES;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineCollection;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherLine;
import org.folio.rest.jaxrs.model.VoucherLineCollection;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.http.Header;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SearchLinesByAcqUnitsTest extends TestBase {
  private static final Logger logger = LoggerFactory.getLogger(SearchLinesByAcqUnitsTest.class);

  private static final String TENANT_NAME = "linesearch";
  private static final Header NEW_TENANT = new Header(OKAPI_HEADER_TENANT, TENANT_NAME);
  private static final String NOT_EMPTY_ACQ_UNITS_QUERY = "?query=acqUnitIds=\"\" NOT acqUnitIds==[]";
  private static final String QUERY_ACQ_UNIT_BY_IDS = "?query=acqUnitIds=(%s and %s)";

  @BeforeAll
  public static void before() throws MalformedURLException {
    logger.info("Create a new tenant loading the sample data");
    prepareTenant(NEW_TENANT, true);
  }

  @AfterAll
  public static void after() throws MalformedURLException {
    logger.info("Delete the created \"linesearch\" tenant");
    deleteTenant(NEW_TENANT);
  }

  @Test
  public void testGetInvoiceLinesByAcqUnits() throws MalformedURLException {
    logger.info("--- mod-invoice-storage invoice-lines: Verify query Invoice Lines by acq units");

    // Check that there are Invoice Line(s) associated with an invoice which has acquisition unit(s) assigned

    String endpoint = INVOICE_LINES.getEndpoint() + NOT_EMPTY_ACQ_UNITS_QUERY;
    List<InvoiceLine> invoiceLines = getLines(endpoint, InvoiceLineCollection.class).getInvoiceLines();
    assertThat(invoiceLines, hasSize(2));
    String invoiceId = invoiceLines.get(0).getInvoiceId();
    
    logger.info("--- mod-invoice-storage invoices test: verify that no duplicated lines returned if more then one acq unit assigned");

    // 1. Get an invoice with acquisition unit(s) assigned
    Invoice invoice = (Invoice) getById(TestEntities.INVOICES, invoiceId);
    assertThat(invoice.getAcqUnitIds(), not(empty()));

    // 2. Update invoice adding one more acq unit
    String acqUnitId = UUID.randomUUID().toString();
    invoice.getAcqUnitIds().add(acqUnitId);
    putData(TestEntities.INVOICES.getEndpointWithId(), invoiceId, JsonObject.mapFrom(invoice).encode(), NEW_TENANT);

    // Search lines by existing and new acq units
    // Check that acq units can be used as search query for `invoice-lines` endpoint
    endpoint = INVOICE_LINES.getEndpoint() + String.format(QUERY_ACQ_UNIT_BY_IDS, invoice.getAcqUnitIds().get(0), acqUnitId);
    List<InvoiceLine> invoiceLines1 = getLines(endpoint, InvoiceLineCollection.class).getInvoiceLines();

    assertThat(invoiceLines1, hasSize(2));
    List<String> invoiceIds = invoiceLines1.stream().map(InvoiceLine::getInvoiceId).collect(Collectors.toList());
    assertThat(invoiceIds, everyItem(is(invoiceId)));
  }

  @Test
  public void testGetVoucherLinesByAcqUnits() throws MalformedURLException {
    logger.info("--- mod-invoice-storage voucher-lines: Verify query Voucher Lines by acq units");

    // Check that there are Voucher Line(s) associated with an voucher which has acquisition unit(s) assigned
    String endpoint = VOUCHER_LINES.getEndpoint() + NOT_EMPTY_ACQ_UNITS_QUERY;
    List<VoucherLine> voucherLines =  getLines(endpoint, VoucherLineCollection.class).getVoucherLines();

    assertThat(voucherLines, hasSize(1));
    String voucherId = voucherLines.get(0).getVoucherId();

    logger.info("--- mod-invoice-storage vouchers test: verify that no duplicated lines returned if more then one acq unit assigned");

    // 1. Get an voucher with acquisition unit(s) assigned
    Voucher voucher = (Voucher) getById(VOUCHERS, voucherId);
    assertThat(voucher.getAcqUnitIds(), not(empty()));

    // 2. Update voucher adding one more acq unit
    String acqUnitId = UUID.randomUUID().toString();
    voucher.getAcqUnitIds().add(acqUnitId);
    putData(VOUCHERS.getEndpointWithId(), voucherId, JsonObject.mapFrom(voucher).encode(), NEW_TENANT);

    // Search lines by existing and new acq units
    // Check that acq units can be used as search query for `voucher-lines` endpoint
    endpoint = VOUCHER_LINES.getEndpoint() + String.format(QUERY_ACQ_UNIT_BY_IDS, voucher.getAcqUnitIds().get(0), acqUnitId);
    voucherLines =  getLines(endpoint, VoucherLineCollection.class).getVoucherLines();

    assertThat(voucherLines, hasSize(1));
    List<String> invoiceIds = voucherLines.stream().map(VoucherLine::getVoucherId).collect(Collectors.toList());
    assertThat(invoiceIds, everyItem(is(voucherId)));
  }

  private Object getById(TestEntities entity, String id) throws MalformedURLException {
    return getDataById(entity.getEndpointWithId(), id, NEW_TENANT)
      .then().log().ifValidationFails()
      .statusCode(200)
      .body(ID, equalTo(id))
      .extract().as(entity.getClazz());
  }

  private <T> T getLines(String endpoint, Class<T> tClass) throws MalformedURLException {
    return getData(endpoint, NEW_TENANT)
      .then()
      .statusCode(200)
      .extract()
      .as(tClass);
  }


}
