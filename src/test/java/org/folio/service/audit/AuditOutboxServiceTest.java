package org.folio.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.Voucher;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.Json;

class AuditOutboxServiceTest {

  private final AuditOutboxService service = new AuditOutboxService(null, null);

  @Test
  void decodesWrapperPayloadWithBothEntities() {
    var current = invoice("v-new");
    var original = invoice("v-old");
    var payload = Json.encode(new AuditEntityWrapper<>(current, original, null));

    AuditEntityWrapper<Invoice, Void> result = service.decodePayload(payload, Invoice.class, Void.class);

    assertNotNull(result.entity());
    assertEquals("v-new", result.entity().getVendorInvoiceNo());
    assertNotNull(result.originalEntity());
    assertEquals("v-old", result.originalEntity().getVendorInvoiceNo());
  }

  @Test
  void decodesWrapperPayloadWithNullOriginal() {
    var current = invoice("v");
    var payload = Json.encode(new AuditEntityWrapper<>(current, null, null));

    AuditEntityWrapper<Invoice, Void> result = service.decodePayload(payload, Invoice.class, Void.class);

    assertEquals("v", result.entity().getVendorInvoiceNo());
    assertNull(result.originalEntity());
  }

  @Test
  void decodesBareEntityPayload() {
    var current = invoice("bare");
    var bareEntityPayload = Json.encode(current);

    AuditEntityWrapper<Invoice, Void> result = service.decodePayload(bareEntityPayload, Invoice.class, Void.class);

    assertNotNull(result.entity());
    assertEquals("bare", result.entity().getVendorInvoiceNo());
    assertNull(result.originalEntity());
  }

  @Test
  void decodesInvoiceLineWrapperPayloadWithParentInvoice() {
    var current = new InvoiceLine().withId(UUID.randomUUID().toString()).withInvoiceLineNumber("3");
    var payload = Json.encode(new AuditEntityWrapper<>(current, null, invoice("INV-42")));

    AuditEntityWrapper<InvoiceLine, Invoice> result = service.decodePayload(payload, InvoiceLine.class, Invoice.class);

    assertEquals("3", result.entity().getInvoiceLineNumber());
    assertEquals("INV-42", result.parentEntity().getVendorInvoiceNo());
  }

  @Test
  void decodesBareInvoiceLinePayload() {
    var current = new InvoiceLine().withId(UUID.randomUUID().toString()).withInvoiceLineNumber("3");

    AuditEntityWrapper<InvoiceLine, Invoice> result = service.decodePayload(Json.encode(current), InvoiceLine.class, Invoice.class);

    assertEquals("3", result.entity().getInvoiceLineNumber());
    assertNull(result.originalEntity());
    assertNull(result.parentEntity());
  }

  @Test
  void decodesVoucherWrapperPayload() {
    var current = voucher("1000");
    var payload = Json.encode(new AuditEntityWrapper<>(current, null, null));

    AuditEntityWrapper<Voucher, Void> result = service.decodePayload(payload, Voucher.class, Void.class);

    assertNotNull(result.entity());
    assertEquals("1000", result.entity().getVoucherNumber());
    assertEquals(List.of("acq-unit-1", "acq-unit-2"), result.entity().getAcqUnitIds());
    assertNull(result.originalEntity());
  }

  @Test
  void decodesVoucherWrapperPayloadWithBothEntities() {
    var current = voucher("1001");
    var original = voucher("1000");
    var payload = Json.encode(new AuditEntityWrapper<>(current, original, null));

    AuditEntityWrapper<Voucher, Void> result = service.decodePayload(payload, Voucher.class, Void.class);

    assertEquals("1001", result.entity().getVoucherNumber());
    assertNotNull(result.originalEntity());
    assertEquals("1000", result.originalEntity().getVoucherNumber());
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