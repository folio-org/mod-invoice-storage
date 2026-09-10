package org.folio.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.Voucher;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.Json;

class AuditOutboxServiceTest {

  private final AuditOutboxService service = new AuditOutboxService(null, null);

  @Test
  void decodesWrapperPayloadWithBothEntities() {
    var current = invoice("v-new");
    var original = invoice("v-old");
    var payload = Json.encode(AuditEntityWrapper.of(current, original));

    AuditEntityWrapper<Invoice> result = service.decodePayload(payload, Invoice.class);

    assertNotNull(result.getEntity());
    assertEquals("v-new", result.getEntity().getVendorInvoiceNo());
    assertNotNull(result.getOriginalEntity());
    assertEquals("v-old", result.getOriginalEntity().getVendorInvoiceNo());
  }

  @Test
  void decodesWrapperPayloadWithNullOriginal() {
    var current = invoice("v");
    var payload = Json.encode(AuditEntityWrapper.of(current, null));

    AuditEntityWrapper<Invoice> result = service.decodePayload(payload, Invoice.class);

    assertEquals("v", result.getEntity().getVendorInvoiceNo());
    assertNull(result.getOriginalEntity());
  }

  @Test
  void fallsBackToBareEntityForLegacyPayload() {
    var current = invoice("legacy");
    var legacyPayload = Json.encode(current);

    AuditEntityWrapper<Invoice> result = service.decodePayload(legacyPayload, Invoice.class);

    assertNotNull(result.getEntity());
    assertEquals("legacy", result.getEntity().getVendorInvoiceNo());
    assertNull(result.getOriginalEntity());
  }

  @Test
  void decodesVoucherWrapperPayload() {
    var current = voucher("1000");
    var payload = Json.encode(AuditEntityWrapper.of(current, null));

    AuditEntityWrapper<Voucher> result = service.decodePayload(payload, Voucher.class);

    assertNotNull(result.getEntity());
    assertEquals("1000", result.getEntity().getVoucherNumber());
    assertEquals(List.of("acq-unit-1", "acq-unit-2"), result.getEntity().getAcqUnitIds());
    assertNull(result.getOriginalEntity());
  }

  @Test
  void decodesVoucherWrapperPayloadWithBothEntities() {
    var current = voucher("1001");
    var original = voucher("1000");
    var payload = Json.encode(AuditEntityWrapper.of(current, original));

    AuditEntityWrapper<Voucher> result = service.decodePayload(payload, Voucher.class);

    assertEquals("1001", result.getEntity().getVoucherNumber());
    assertNotNull(result.getOriginalEntity());
    assertEquals("1000", result.getOriginalEntity().getVoucherNumber());
  }

  private Invoice invoice(String vendorInvoiceNo) {
    return new Invoice()
      .withId(UUID.randomUUID().toString())
      .withVendorInvoiceNo(vendorInvoiceNo);
  }

  private Voucher voucher(String voucherNumber) {
    return new Voucher()
      .withId(UUID.randomUUID().toString())
      .withInvoiceId(UUID.randomUUID().toString())
      .withVoucherNumber(voucherNumber)
      .withAcqUnitIds(List.of("acq-unit-1", "acq-unit-2"));
  }

}