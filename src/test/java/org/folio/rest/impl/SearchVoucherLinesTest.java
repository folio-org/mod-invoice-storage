package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.folio.rest.utils.TestEntities.ACQUISITIONS_UNIT_ASSIGNMENTS;
import static org.folio.rest.utils.TestEntities.VOUCHER_LINES;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.Assert.assertThat;

import io.restassured.http.Header;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.AcquisitionsUnitAssignment;
import org.folio.rest.jaxrs.model.VoucherLine;
import org.folio.rest.jaxrs.model.VoucherLineCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SearchVoucherLinesTest extends TestBase {
  private static final Logger logger = LoggerFactory.getLogger(SearchVoucherLinesTest.class);

  private static final String VOUCHER_LINES_ENDPOINT = "/voucher-storage/voucher-lines";
  private static final String TENANT_NAME = "voucherlinesearch";
  private static final Header NEW_TENANT = new Header(OKAPI_HEADER_TENANT, TENANT_NAME);

  @BeforeAll
  public static void before() throws MalformedURLException {
    logger.info("Create a new tenant loading the sample data");
    prepareTenant(NEW_TENANT, true);
  }

  @AfterAll
  public static void after() throws MalformedURLException {
    logger.info("Delete the created \"voucherlinesearch\" tenant");
    deleteTenant(NEW_TENANT);
  }

  @Test
  public void testGetVoucherLines() throws MalformedURLException {
    logger.info("--- mod-invoice-storage voucher-lines: Verify view was created and contains all sample records");
    verifyCollectionQuantity(VOUCHER_LINES.getEndpoint(), VOUCHER_LINES.getInitialQuantity(), NEW_TENANT);
  }

  @Test
  public void testGetVoucherLinesWithQuery() throws MalformedURLException {
    logger.info("--- mod-invoice-storage voucher-lines: Verify query on fields from PO Lines");
    List<VoucherLine> voucherLines = queryAndGetVoucherLines(VOUCHER_LINES.getEndpoint() + "?query=externalAccountNumber==54321098 and amount=23.45");
    assertThat(voucherLines, hasSize(1));
    assertThat(voucherLines.get(0).getExternalAccountNumber(), is("54321098"));
    assertThat(voucherLines.get(0).getAmount(), is(23.45));
  }

  @Test
  public void testGetVoucherLinesWithLimit() throws MalformedURLException {
    logger.info("--- mod-invoice-storage voucher-lines: Verify the limit parameter");
    List<VoucherLine> filteredByPoAndP0LineFields = queryAndGetVoucherLines(VOUCHER_LINES.getEndpoint() + "?limit=5");
    assertThat(filteredByPoAndP0LineFields, hasSize(1));
  }

  @Test
  public void testGetVoucherLinesByAcqUnits() throws MalformedURLException {
    logger.info("--- mod-invoice-storage voucher-lines: Verify query Voucher Lines by acq units");

    AcquisitionsUnitAssignment acqUnitAssignment = new JsonObject(getFile("data/acquisitions-unit-assignments/AUA-ca851994.json"))
      .mapTo(AcquisitionsUnitAssignment.class);
    String acqUnitQuery = "?query=acquisitionsUnitId==" + acqUnitAssignment.getAcquisitionsUnitId();

    verifySearchByAcqUnit(acqUnitQuery, acqUnitAssignment.getRecordId());

    logger.info("--- mod-invoice-storage voucher lines test: verify that no duplicated lines returned if more then one acq unit assigned");

    // 1. Create new acq unit
    String acqUnitId = UUID.randomUUID().toString();
    // 2. Assign created acq unit to the same order
    AcquisitionsUnitAssignment acqUnitAssignment2 = new AcquisitionsUnitAssignment()
      .withAcquisitionsUnitId(acqUnitId)
      .withRecordId(acqUnitAssignment.getRecordId());
    createEntity(ACQUISITIONS_UNIT_ASSIGNMENTS.getEndpoint(), JsonObject.mapFrom(acqUnitAssignment2).encode(), NEW_TENANT);

    // Search lines by 2 acq units
    verifySearchByAcqUnit(acqUnitQuery + " or acquisitionsUnitId==" + acqUnitId, acqUnitAssignment.getRecordId());
  }


  private void verifySearchByAcqUnit(String acqUnitQuery, String voucherId) throws MalformedURLException {
    // Check that acq units can be used as search query for `voucher-lines` endpoint
    List<VoucherLine> voucherLines  = queryAndGetVoucherLines(VOUCHER_LINES.getEndpoint() + acqUnitQuery);
    assertThat(voucherLines, hasSize(1));
    verifyFilteredLinesByAcqUnits(voucherLines, voucherId);
  }

  private void verifyFilteredLinesByAcqUnits(List<VoucherLine> voucherLines, String voucherId) {
    List<String> voucherIds = voucherLines.stream().map(VoucherLine::getVoucherId).collect(Collectors.toList());
    assertThat(voucherIds, everyItem(is(voucherId)));
  }

  private List<VoucherLine> queryAndGetVoucherLines(String endpoint) throws MalformedURLException {
    return getData(endpoint, NEW_TENANT).then()
      .statusCode(200)
      .extract()
       .as(VoucherLineCollection.class)
       .getVoucherLines();
  }
}
