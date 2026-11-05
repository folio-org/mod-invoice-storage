package org.folio.service.calendar;

import java.util.Map;

import org.folio.kafka.KafkaConfig;
import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.SimpleKafkaProducerManager;
import org.folio.kafka.services.KafkaProducerRecordBuilder;
import org.folio.rest.jaxrs.model.CalendarMatchEvent;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class CalendarMatchProducer {

  public static final String CALENDAR_MATCH_TOPIC = "NOTIFICATION_CALENDAR_MATCH";
  static final String SOURCE_MODULE_HEADER = "x-folio-source-module";

  private final KafkaConfig kafkaConfig;

  /**
   * Sends a calendar match event, keyed by the source module.
   *
   * @param event        the match event
   * @param okapiHeaders the okapi headers of the trigger event
   * @return succeeded future once the broker acknowledged the event, failed future in another case
   */
  public Future<Void> sendMatchEvent(CalendarMatchEvent event, Map<String, String> okapiHeaders) {
    var topicName = KafkaTopicNameHelper.formatTopicName(kafkaConfig.getEnvId(),
      KafkaTopicNameHelper.getDefaultNameSpace(), event.getTenant(), CALENDAR_MATCH_TOPIC);
    KafkaProducerRecord<String, String> kafkaProducerRecord = new KafkaProducerRecordBuilder<String, Object>(event.getTenant())
      .key(event.getSourceModule())
      .value(event)
      .topic(topicName)
      .propagateOkapiHeaders(okapiHeaders)
      .header(SOURCE_MODULE_HEADER, event.getSourceModule())
      .build();
    var producerManager = new SimpleKafkaProducerManager(Vertx.currentContext().owner(), kafkaConfig);
    KafkaProducer<String, String> producer = producerManager.createShared(topicName);
    return producer.send(kafkaProducerRecord)
      .onComplete(reply -> producer.end().onComplete(v -> producer.close()))
      .<Void>mapEmpty()
      .onSuccess(v -> log.info("sendMatchEvent:: Event with id: '{}' and {} matches has been sent to kafka topic '{}'",
        event.getId(), event.getMatches().size(), topicName))
      .onFailure(t -> log.error("sendMatchEvent:: Failed to send event with id: '{}' for trigger event: '{}' to kafka topic '{}'",
        event.getId(), event.getTriggerEventId(), topicName, t));
  }

}
