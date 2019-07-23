package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.utils.TenantApiTestUtil.deleteTenant;
import static org.folio.rest.utils.TenantApiTestUtil.prepareTenant;
import static org.folio.rest.utils.TestEntities.VOUCHERS;
import static org.folio.rest.utils.TestEntities.VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS;
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
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherCollection;
import org.folio.rest.jaxrs.model.VoucherLine;
import org.folio.rest.jaxrs.model.VoucherLineCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SearchByAcqUnitsTest extends TestBase {
  private static final Logger logger = LoggerFactory.getLogger(SearchByAcqUnitsTest.class);

  private static final String TENANT_NAME = "acqunitsearch";
  private static final Header NEW_TENANT = new Header(OKAPI_HEADER_TENANT, TENANT_NAME);

  @BeforeAll
  public static void before() throws MalformedURLException {
    logger.info("Create a new tenant loading the sample data");
    prepareTenant(NEW_TENANT, true);
  }

  @AfterAll
  public static void after() throws MalformedURLException {
    logger.info("Delete the created \"acqunitsearch\" tenant");
    deleteTenant(NEW_TENANT);
  }

  @Test
  public void testSearchByVoucherAcqUnitsAssigns() throws MalformedURLException {
    logger.info("--- mod-invoice-storage: Verify query Vouchers and Voucher Lines by acq units");

    AcquisitionsUnitAssignment acqUnitAssignment = new JsonObject(getFile(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS.getSampleFileName()))
      .mapTo(AcquisitionsUnitAssignment.class);
    String acqUnitQuery = "?query=acquisitionsUnitId==" + acqUnitAssignment.getAcquisitionsUnitId();

    verifySearchVoucherLinesByAcqUnit(acqUnitQuery, acqUnitAssignment.getRecordId());
    verifySearchVouchersByAcqUnit(acqUnitQuery, acqUnitAssignment.getRecordId());
    logger.info("--- mod-invoice-storage: verify that no duplicated lines returned if more then one acq unit assigned");

    // 1. Create new acq unit
    String acqUnitId = UUID.randomUUID().toString();
    // 2. Assign created acq unit to the same voucher
    AcquisitionsUnitAssignment acqUnitAssignment2 = new AcquisitionsUnitAssignment()
      .withAcquisitionsUnitId(acqUnitId)
      .withRecordId(acqUnitAssignment.getRecordId());
    createEntity(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS.getEndpoint(), JsonObject.mapFrom(acqUnitAssignment2).encode(), NEW_TENANT);

    // Search by 2 acq units
    verifySearchVoucherLinesByAcqUnit(acqUnitQuery + " or acquisitionsUnitId==" + acqUnitId, acqUnitAssignment.getRecordId());
    verifySearchVouchersByAcqUnit(acqUnitQuery + " or acquisitionsUnitId==" + acqUnitId, acqUnitAssignment.getRecordId());
  }


  private void verifySearchVoucherLinesByAcqUnit(String acqUnitQuery, String voucherId) throws MalformedURLException {
    // Check that acq units can be used as search query for `voucher-lines` endpoint
    List<VoucherLine> voucherLines  = queryAndGetEntries(VOUCHER_LINES.getEndpoint() + acqUnitQuery, VoucherLineCollection.class).getVoucherLines();
    assertThat(voucherLines, hasSize(1));
    verifyFilteredLinesByAcqUnits(voucherLines, voucherId);
  }

  private void verifySearchVouchersByAcqUnit(String acqUnitQuery, String id) throws MalformedURLException {
    // Check that acq units can be used as search query for `vouchers` endpoint
    List<Voucher> vouchers  = queryAndGetEntries(VOUCHERS.getEndpoint() + acqUnitQuery, VoucherCollection.class).getVouchers();
    assertThat(vouchers, hasSize(1));
    verifyFilteredVouchersByAcqUnits(vouchers, id);
  }

  private void verifyFilteredVouchersByAcqUnits(List<Voucher> vouchers, String id) {
    List<String> ids = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
    assertThat(ids, everyItem(is(id)));
  }

  private void verifyFilteredLinesByAcqUnits(List<VoucherLine> voucherLines, String voucherId) {
    List<String> voucherIds = voucherLines.stream().map(VoucherLine::getVoucherId).collect(Collectors.toList());
    assertThat(voucherIds, everyItem(is(voucherId)));
  }

  private <T> T queryAndGetEntries(String endpoint, Class<T> collectionClass) throws MalformedURLException {
    return getData(endpoint, NEW_TENANT).then()
      .statusCode(200)
      .extract()
       .as(collectionClass);
  }
}
