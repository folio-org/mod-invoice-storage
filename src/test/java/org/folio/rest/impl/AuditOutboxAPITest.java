package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.folio.rest.impl.StorageTestSuite.storageUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.EventTopic;
import org.folio.rest.jaxrs.model.VoucherAuditEvent;
import org.folio.rest.utils.TestData;
import org.folio.rest.utils.TestEntities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class AuditOutboxAPITest extends TestBase {

  public static final String AUDIT_OUTBOX_ENDPOINT = "/invoice-storage/audit-outbox/process";

  private static final String DATE_BEFORE_EDIT = "2019-05-05T00:00:00.000+0000";
  private static final String DATE_AFTER_EDIT = "2019-06-06T00:00:00.000+0000";

  /** Entities created by a test, newest first, so teardown deletes children before their parents. */
  private final Deque<Pair<String, String>> createdEntities = new ArrayDeque<>();

  @AfterEach
  void deleteCreatedEntities() throws MalformedURLException {
    while (!createdEntities.isEmpty()) {
      var entity = createdEntities.pop();
      deleteData(entity.getLeft(), entity.getRight())
        .then().log().ifValidationFails()
        .statusCode(204);
    }
  }

  @Test
  void testPostInvoiceStorageAuditOutboxProcess() throws MalformedURLException {
    processOutbox();
  }

  @Test
  void voucherCreatePublishesAuditEventThroughOutbox() throws MalformedURLException {
    var acqUnitIds = List.of("f2d5a3b4-4ee3-4f47-9d0f-1e9c60a7f1a1", "23a0d0d2-1ff5-4c56-8b9b-3f0e2c0f31c3");
    JsonObject voucher = givenVoucher(new JsonObject()
      .put("voucherNumber", "9000")
      .put("acqUnitIds", acqUnitIds));

    // the create path triggers the poll itself; running it again must stay green and be a no-op
    processOutbox();

    JsonObject event = findVoucherEvent(voucher.getString(ID), VoucherAuditEvent.Action.CREATE);
    assertNotNull(event.getString("id"));
    assertNotNull(event.getString("eventDate"));
    assertNotNull(event.getString("actionDate"));
    assertEquals(USER_ID_HEADER.getValue(), event.getString("userId"));
    assertFalse(event.containsKey("originalVoucherSnapshot"));

    JsonObject snapshot = event.getJsonObject("voucherSnapshot");
    assertEquals("9000", snapshot.getString("voucherNumber"));
    assertEquals(voucher.getString("disbursementNumber"), snapshot.getString("disbursementNumber"));
    assertEquals(toInstant(voucher.getString("disbursementDate")), toInstant(snapshot.getString("disbursementDate")));
    assertEquals(voucher.getDouble("disbursementAmount"), snapshot.getDouble("disbursementAmount"));
    assertEquals(voucher.getJsonArray("acqUnitIds"), snapshot.getJsonArray("acqUnitIds"));
    assertFalse(snapshot.containsKey("metadata"));
  }

  @Test
  void voucherEditPublishesAuditEventWithBothSnapshots() throws MalformedURLException {
    JsonObject original = givenVoucher(new JsonObject()
      .put("voucherNumber", "9100")
      .put("disbursementNumber", "EFT-before")
      .put("disbursementDate", DATE_BEFORE_EDIT)
      .put("disbursementAmount", 100.00));
    String voucherId = original.getString(ID);

    updateVoucher(voucherId, original.copy()
      .put("voucherNumber", "9101")
      .put("disbursementNumber", "EFT-after")
      .put("disbursementDate", DATE_AFTER_EDIT)
      .put("disbursementAmount", 250.00));
    processOutbox();

    JsonObject event = findVoucherEvent(voucherId, VoucherAuditEvent.Action.EDIT);
    assertEquals(voucherId, event.getString("voucherId"));
    assertNotNull(event.getString("eventDate"));
    assertNotNull(event.getString("actionDate"));
    assertEquals(USER_ID_HEADER.getValue(), event.getString("userId"));

    JsonObject post = event.getJsonObject("voucherSnapshot");
    JsonObject pre = event.getJsonObject("originalVoucherSnapshot");
    assertNotNull(pre, "Edit event must carry the pre-edit snapshot");
    assertEquals(voucherId, post.getString(ID));
    assertEquals(post.getString(ID), pre.getString(ID));

    assertEquals("9101", post.getString("voucherNumber"));
    assertEquals("9100", pre.getString("voucherNumber"));
    assertEquals("EFT-after", post.getString("disbursementNumber"));
    assertEquals("EFT-before", pre.getString("disbursementNumber"));
    assertEquals(250.00, post.getDouble("disbursementAmount"));
    assertEquals(100.00, pre.getDouble("disbursementAmount"));
    assertEquals(toInstant(DATE_AFTER_EDIT), toInstant(post.getString("disbursementDate")));
    assertEquals(toInstant(DATE_BEFORE_EDIT), toInstant(pre.getString("disbursementDate")));

    assertFalse(post.containsKey("metadata"));
    assertFalse(pre.containsKey("metadata"));
  }

  /**
   * Creates a voucher from the default sample with {@code overrides} applied, along with the invoice it
   * references. Both are deleted after the test.
   *
   * @return the created voucher, carrying its generated id
   */
  private JsonObject givenVoucher(JsonObject overrides) throws MalformedURLException {
    JsonObject invoice = new JsonObject(getFile(TestData.Invoice.DEFAULT)).put(ID, UUID.randomUUID().toString());
    String invoiceId = createTrackedEntity(TestEntities.INVOICE, invoice);

    JsonObject voucher = new JsonObject(getFile(TestData.Voucher.DEFAULT))
      .put(ID, UUID.randomUUID().toString())
      .put("invoiceId", invoiceId)
      .mergeIn(overrides);
    return voucher.put(ID, createTrackedEntity(TestEntities.VOUCHER, voucher));
  }

  private String createTrackedEntity(TestEntities entity, JsonObject body) throws MalformedURLException {
    String id = createEntity(entity.getEndpoint(), body.encode());
    createdEntities.push(Pair.of(entity.getEndpointWithId(), id));
    return id;
  }

  private void updateVoucher(String voucherId, JsonObject voucher) throws MalformedURLException {
    given()
      .spec(commonRequestSpec())
      .pathParam(ID, voucherId)
      .body(voucher.encode())
      .when()
      .put(storageUrl(TestEntities.VOUCHER.getEndpointWithId()))
      .then().log().ifValidationFails()
      .statusCode(204);
  }

  private void processOutbox() throws MalformedURLException {
    given()
      .spec(commonRequestSpec())
      .when()
      .post(storageUrl(AUDIT_OUTBOX_ENDPOINT))
      .then().log().ifValidationFails()
      .statusCode(200);
  }

  private JsonObject findVoucherEvent(String voucherId, VoucherAuditEvent.Action action) {
    List<String> events = StorageTestSuite.checkKafkaEventSent(TENANT_HEADER.getValue(),
      EventTopic.ACQ_VOUCHER_CHANGED.value());
    return events.stream()
      .map(JsonObject::new)
      .filter(event -> voucherId.equals(event.getString("voucherId")) && action.value().equals(event.getString("action")))
      .findFirst()
      .orElseGet(() -> fail("No %s event for voucher %s on %s (%d event(s) observed on the topic)"
        .formatted(action.value(), voucherId, EventTopic.ACQ_VOUCHER_CHANGED, events.size())));
  }

  /** Voucher dates round-trip through Kafka as +00:00 rather than the +0000 the samples use. */
  private static Instant toInstant(String date) {
    return Instant.parse(date.replace("+0000", "Z").replace("+00:00", "Z"));
  }

}
