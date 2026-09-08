package org.folio.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherAuditEvent;
import org.junit.jupiter.api.Test;

class AuditEventProducerTest {

  private final AuditEventProducer producer = new AuditEventProducer(null);

  @Test
  void invoiceEditEventCarriesOriginalSnapshot() {
    var original = getInvoice("vendor-old", "Open");
    var updated = getInvoice("vendor-new", "Reviewed");

    InvoiceAuditEvent event = producer.getAuditEvent(updated, original, InvoiceAuditEvent.Action.EDIT);

    assertEquals(InvoiceAuditEvent.Action.EDIT, event.getAction());
    assertNotNull(event.getInvoiceSnapshot());
    assertEquals("vendor-new", event.getInvoiceSnapshot().getVendorInvoiceNo());
    assertEquals(Invoice.Status.REVIEWED, event.getInvoiceSnapshot().getStatus());

    assertNotNull(event.getOriginalInvoiceSnapshot());
    assertEquals("vendor-old", event.getOriginalInvoiceSnapshot().getVendorInvoiceNo());
    assertEquals(Invoice.Status.OPEN, event.getOriginalInvoiceSnapshot().getStatus());
  }

  @Test
  void invoiceCreateEventOmitsOriginalSnapshot() {
    InvoiceAuditEvent event = producer.getAuditEvent(getInvoice("v", "Open"), null, InvoiceAuditEvent.Action.CREATE);

    assertEquals(InvoiceAuditEvent.Action.CREATE, event.getAction());
    assertNotNull(event.getInvoiceSnapshot());
    assertNull(event.getOriginalInvoiceSnapshot());
  }

  @Test
  void invoiceLineEditEventCarriesOriginalSnapshot() {
    var original = getInvoiceLine("desc-old", 10d);
    var updated = getInvoiceLine("desc-new", 25d);

    InvoiceLineAuditEvent event = producer.getAuditEvent(updated, original, InvoiceLineAuditEvent.Action.EDIT);

    assertEquals(InvoiceLineAuditEvent.Action.EDIT, event.getAction());
    assertNotNull(event.getInvoiceLineSnapshot());
    assertEquals("desc-new", event.getInvoiceLineSnapshot().getDescription());
    assertEquals(25d, event.getInvoiceLineSnapshot().getSubTotal());

    assertNotNull(event.getOriginalInvoiceLineSnapshot());
    assertEquals("desc-old", event.getOriginalInvoiceLineSnapshot().getDescription());
    assertEquals(10d, event.getOriginalInvoiceLineSnapshot().getSubTotal());
  }

  @Test
  void invoiceLineCreateEventOmitsOriginalSnapshot() {
    InvoiceLineAuditEvent event = producer.getAuditEvent(getInvoiceLine("d", 1d), null, InvoiceLineAuditEvent.Action.CREATE);

    assertEquals(InvoiceLineAuditEvent.Action.CREATE, event.getAction());
    assertNotNull(event.getInvoiceLineSnapshot());
    assertNull(event.getOriginalInvoiceLineSnapshot());
  }

  @Test
  void voucherCreateEventOmitsOriginalSnapshot() {
    var voucher = getVoucher("1000", "EFT546789", 5d);

    VoucherAuditEvent event = producer.getAuditEvent(voucher, null, VoucherAuditEvent.Action.CREATE);

    assertNotNull(event.getId());
    assertEquals(VoucherAuditEvent.Action.CREATE, event.getAction());
    assertEquals(voucher.getId(), event.getVoucherId());
    assertNotNull(event.getEventDate());
    assertNotNull(event.getActionDate());
    assertNotNull(event.getUserId());

    assertNotNull(event.getVoucherSnapshot());
    assertEquals("1000", event.getVoucherSnapshot().getVoucherNumber());
    assertEquals("EFT546789", event.getVoucherSnapshot().getDisbursementNumber());
    assertEquals(5d, event.getVoucherSnapshot().getDisbursementAmount());
    assertEquals(List.of("acq-unit-1", "acq-unit-2"), event.getVoucherSnapshot().getAcqUnitIds());
    assertNull(event.getVoucherSnapshot().getMetadata());

    assertNull(event.getOriginalVoucherSnapshot());
  }

  @Test
  void voucherEditEventCarriesOriginalSnapshot() {
    var original = getVoucher("1000", "EFT-old", 5d);
    var updated = getVoucher("1001", "EFT-new", 10d);

    VoucherAuditEvent event = producer.getAuditEvent(updated, original, VoucherAuditEvent.Action.EDIT);

    assertEquals(VoucherAuditEvent.Action.EDIT, event.getAction());
    assertEquals("1001", event.getVoucherSnapshot().getVoucherNumber());
    assertEquals("EFT-new", event.getVoucherSnapshot().getDisbursementNumber());

    assertNotNull(event.getOriginalVoucherSnapshot());
    assertEquals("1000", event.getOriginalVoucherSnapshot().getVoucherNumber());
    assertEquals("EFT-old", event.getOriginalVoucherSnapshot().getDisbursementNumber());
    assertNull(event.getOriginalVoucherSnapshot().getMetadata());
  }

  private Invoice getInvoice(String vendorInvoiceNo, String status) {
    return new Invoice()
      .withId(UUID.randomUUID().toString())
      .withVendorInvoiceNo(vendorInvoiceNo)
      .withStatus(Invoice.Status.fromValue(status))
      .withMetadata(getMetadata());
  }

  private InvoiceLine getInvoiceLine(String description, double subTotal) {
    return new InvoiceLine()
      .withId(UUID.randomUUID().toString())
      .withInvoiceId(UUID.randomUUID().toString())
      .withDescription(description)
      .withSubTotal(subTotal)
      .withMetadata(getMetadata());
  }

  private Voucher getVoucher(String voucherNumber, String disbursementNumber, double disbursementAmount) {
    return new Voucher()
      .withId(UUID.randomUUID().toString())
      .withInvoiceId(UUID.randomUUID().toString())
      .withVoucherNumber(voucherNumber)
      .withDisbursementNumber(disbursementNumber)
      .withDisbursementDate(new Date())
      .withDisbursementAmount(disbursementAmount)
      .withAcqUnitIds(List.of("acq-unit-1", "acq-unit-2"))
      .withMetadata(getMetadata());
  }

  private Metadata getMetadata() {
    return new Metadata()
      .withUpdatedDate(new Date())
      .withUpdatedByUserId(UUID.randomUUID().toString());
  }
}
