package org.folio.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.folio.rest.jaxrs.model.Invoice;
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

  private Invoice invoice(String vendorInvoiceNo) {
    return new Invoice()
      .withId(UUID.randomUUID().toString())
      .withVendorInvoiceNo(vendorInvoiceNo);
  }
}