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
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
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
    assertNotNull(snapshot.getJsonObject("metadata"), "Snapshot must keep the metadata the consumer needs");
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

    assertNotNull(post.getJsonObject("metadata"), "Snapshot must keep the metadata the consumer needs");
    assertNotNull(pre.getJsonObject("metadata"));
  }

  @Test
  void voucherEditWithoutIdInBodyStillCarriesIdInBothSnapshots() throws MalformedURLException {
    JsonObject original = givenVoucher(new JsonObject().put("voucherNumber", "9200"));
    String voucherId = original.getString(ID);

    JsonObject bodyWithoutId = original.copy().put("voucherNumber", "9201");
    bodyWithoutId.remove(ID);
    updateVoucher(voucherId, bodyWithoutId);
    processOutbox();

    JsonObject event = findVoucherEvent(voucherId, VoucherAuditEvent.Action.EDIT);
    JsonObject post = event.getJsonObject("voucherSnapshot");
    JsonObject pre = event.getJsonObject("originalVoucherSnapshot");
    assertEquals(voucherId, post.getString(ID), "Post-edit snapshot must carry the path id even when the body omits it");
    assertEquals(voucherId, pre.getString(ID));
    assertEquals("9201", post.getString("voucherNumber"));
    assertEquals("9200", pre.getString("voucherNumber"));
  }

  @Test
  void invoiceEditWithoutIdInBodyStillCarriesIdInBothSnapshots() throws MalformedURLException {
    JsonObject invoice = new JsonObject(getFile(TestData.Invoice.DEFAULT))
      .put(ID, UUID.randomUUID().toString())
      .put("vendorInvoiceNo", "INV-9300");
    String invoiceId = createTrackedEntity(TestEntities.INVOICE, invoice);

    JsonObject bodyWithoutId = invoice.copy().put("vendorInvoiceNo", "INV-9301");
    bodyWithoutId.remove(ID);
    updateEntity(TestEntities.INVOICE, invoiceId, bodyWithoutId);
    processOutbox();

    JsonObject event = findEvent(EventTopic.ACQ_INVOICE_CHANGED, "invoiceId", invoiceId, InvoiceAuditEvent.Action.EDIT.value());
    JsonObject post = event.getJsonObject("invoiceSnapshot");
    JsonObject pre = event.getJsonObject("originalInvoiceSnapshot");
    assertEquals(invoiceId, post.getString(ID), "Post-edit snapshot must carry the path id even when the body omits it");
    assertEquals(invoiceId, pre.getString(ID));
    assertEquals("INV-9301", post.getString("vendorInvoiceNo"));
    assertEquals("INV-9300", pre.getString("vendorInvoiceNo"));
  }

  @Test
  void invoiceLineEditWithoutIdInBodyStillCarriesIdInBothSnapshots() throws MalformedURLException {
    JsonObject invoice = new JsonObject(getFile(TestData.Invoice.DEFAULT)).put(ID, UUID.randomUUID().toString());
    String invoiceId = createTrackedEntity(TestEntities.INVOICE, invoice);

    JsonObject invoiceLine = new JsonObject(getFile(TestData.InvoiceLines.DEFAULT))
      .put(ID, UUID.randomUUID().toString())
      .put("invoiceId", invoiceId)
      .put("description", "line before edit");
    String invoiceLineId = createTrackedEntity(TestEntities.INVOICE_LINES, invoiceLine);

    JsonObject bodyWithoutId = invoiceLine.copy().put("description", "line after edit");
    bodyWithoutId.remove(ID);
    updateEntity(TestEntities.INVOICE_LINES, invoiceLineId, bodyWithoutId);
    processOutbox();

    JsonObject event = findEvent(EventTopic.ACQ_INVOICE_LINE_CHANGED, "invoiceLineId", invoiceLineId,
      InvoiceLineAuditEvent.Action.EDIT.value());
    JsonObject post = event.getJsonObject("invoiceLineSnapshot");
    JsonObject pre = event.getJsonObject("originalInvoiceLineSnapshot");
    assertEquals(invoiceLineId, post.getString(ID), "Post-edit snapshot must carry the path id even when the body omits it");
    assertEquals(invoiceLineId, pre.getString(ID));
    assertEquals("line after edit", post.getString("description"));
    assertEquals("line before edit", pre.getString("description"));
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
    updateEntity(TestEntities.VOUCHER, voucherId, voucher);
  }

  private void updateEntity(TestEntities entity, String entityId, JsonObject body) throws MalformedURLException {
    given()
      .spec(commonRequestSpec())
      .pathParam(ID, entityId)
      .body(body.encode())
      .when()
      .put(storageUrl(entity.getEndpointWithId()))
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
    return findEvent(EventTopic.ACQ_VOUCHER_CHANGED, "voucherId", voucherId, action.value());
  }

  /**
   * Finds the single event on {@code topic} whose {@code idField} and action match, failing the test when the
   * event never reached Kafka - which is what a null entity id on the outbox log looks like from here.
   */
  private JsonObject findEvent(EventTopic topic, String idField, String entityId, String action) {
    List<String> events = StorageTestSuite.checkKafkaEventSent(TENANT_HEADER.getValue(), topic.value());
    return events.stream()
      .map(JsonObject::new)
      .filter(event -> entityId.equals(event.getString(idField)) && action.equals(event.getString("action")))
      .findFirst()
      .orElseGet(() -> fail("No %s event for %s %s on %s (%d event(s) observed on the topic)"
        .formatted(action, idField, entityId, topic, events.size())));
  }

  /** Voucher dates round-trip through Kafka as +00:00 rather than the +0000 the samples use. */
  private static Instant toInstant(String date) {
    return Instant.parse(date.replace("+0000", "Z").replace("+00:00", "Z"));
  }

}
