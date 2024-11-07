package org.folio.service.audit;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.kafka.KafkaConfig;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.SimpleKafkaProducerManager;
import org.folio.kafka.services.KafkaProducerRecordBuilder;
import org.folio.rest.jaxrs.model.EventTopic;
import org.folio.rest.jaxrs.model.Invoice;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class AuditEventProducer {

  private final KafkaConfig kafkaConfig;

  /**
   * Sends event for invoice change(Create, Edit) to kafka.
   * InvoiceId is used as partition key to send all events for particular invoice to the same partition.
   *
   * @param invoice      the event payload
   * @param eventAction  the event action
   * @param okapiHeaders the okapi headers
   * @return future with true if sending was success or failed future in another case
   */
  public Future<Void> sendInvoiceEvent(Invoice invoice, InvoiceAuditEvent.Action eventAction, Map<String, String> okapiHeaders) {
    var event = getAuditEvent(invoice, eventAction);
    log.info("sendInvoiceEvent:: Sending event with id: {} and invoiceId: {} to Kafka", event.getId(), invoice.getId());
    return sendToKafka(EventTopic.ACQ_INVOICE_CHANGED, event.getInvoiceId(), event, okapiHeaders)
      .onFailure(t -> log.warn("sendInvoiceEvent:: Failed to send event with id: {} and invoiceId: {} to Kafka", event.getId(), invoice.getId(), t));
  }

  /**
   * Sends event for invoice line change(Create, Edit) to kafka.
   * InvoiceLineId is used as partition key to send all events for particular invoice line to the same partition.
   *
   * @param invoiceLine  the event payload
   * @param eventAction  the event action
   * @param okapiHeaders the okapi headers
   * @return future with true if sending was success or failed future in another case
   */
  public Future<Void> sendInvoiceLineEvent(InvoiceLine invoiceLine, InvoiceLineAuditEvent.Action eventAction, Map<String, String> okapiHeaders) {
    var event = getAuditEvent(invoiceLine, eventAction);
    log.info("sendInvoiceLineEvent:: Sending event with id: {} and invoiceLineId: {} to Kafka", event.getId(), invoiceLine.getId());
    return sendToKafka(EventTopic.ACQ_INVOICE_LINE_CHANGED, event.getInvoiceLineId(), event, okapiHeaders)
      .onFailure(t -> log.error("sendInvoiceLineEvent:: Failed to send event with id: {} and invoiceLineId: {} to Kafka", event.getId(), invoiceLine.getId(), t));
  }

  private InvoiceAuditEvent getAuditEvent(Invoice invoice, InvoiceAuditEvent.Action eventAction) {
    return new InvoiceAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(eventAction)
      .withInvoiceId(invoice.getId())
      .withEventDate(new Date())
      .withActionDate(invoice.getMetadata().getUpdatedDate())
      .withUserId(invoice.getMetadata().getUpdatedByUserId())
      .withInvoiceSnapshot(invoice.withMetadata(null));
  }

  private InvoiceLineAuditEvent getAuditEvent(InvoiceLine invoiceLine, InvoiceLineAuditEvent.Action eventAction) {
    return new InvoiceLineAuditEvent()
      .withId(UUID.randomUUID().toString())
      .withAction(eventAction)
      .withInvoiceLineId(invoiceLine.getId())
      .withEventDate(new Date())
      .withActionDate(invoiceLine.getMetadata().getUpdatedDate())
      .withUserId(invoiceLine.getMetadata().getUpdatedByUserId())
      .withInvoiceLineSnapshot(invoiceLine.withMetadata(null));
  }

  private Future<Void> sendToKafka(EventTopic eventTopic, String key, Object eventPayload, Map<String, String> okapiHeaders) {
    var tenantId = TenantTool.tenantId(okapiHeaders);
    var topicName = buildTopicName(kafkaConfig.getEnvId(), tenantId, eventTopic.value());
    KafkaProducerRecord<String, String> kafkaProducerRecord = new KafkaProducerRecordBuilder<String, Object>(tenantId)
      .key(key)
      .value(eventPayload)
      .topic(topicName)
      .propagateOkapiHeaders(okapiHeaders)
      .build();

    var producerManager = new SimpleKafkaProducerManager(Vertx.currentContext().owner(), kafkaConfig);
    KafkaProducer<String, String> producer = producerManager.createShared(topicName);
    return producer.send(kafkaProducerRecord)
      .onSuccess(s -> log.info("sendToKafka:: Event for {} with id '{}' has been sent to kafka topic '{}'", eventTopic, key, topicName))
      .onFailure(t -> log.error("Failed to send event for {} with id '{}' to kafka topic '{}'", eventTopic, key, topicName, t))
      .onComplete(reply -> producer.end(v -> producer.close()))
      .mapEmpty();
  }

  private String buildTopicName(String envId, String tenantId, String eventType) {
    return KafkaTopicNameHelper.formatTopicName(envId, KafkaTopicNameHelper.getDefaultNameSpace(), tenantId, eventType);
  }

}
