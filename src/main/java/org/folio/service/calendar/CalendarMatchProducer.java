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
  private static final int MAX_SEND_ATTEMPTS = 3;
  private static final long RETRY_DELAY_MS = 5000;

  private final KafkaConfig kafkaConfig;

  /**
   * Sends a calendar match event, keyed by the source module. A failed send is retried, because the trigger
   * consumer's offset is committed even when its handler fails, so a lost send is not redelivered.
   *
   * @param event        the match event
   * @param okapiHeaders the okapi headers of the trigger event
   * @return succeeded future once the broker acknowledged the event, failed future when every attempt failed
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
    return send(kafkaProducerRecord, topicName, 1)
      .onSuccess(v -> log.info("sendMatchEvent:: Event with id: '{}' and {} matches has been sent to kafka topic '{}'",
        event.getId(), event.getMatches().size(), topicName))
      .onFailure(t -> log.error("sendMatchEvent:: Failed to send event with id: '{}' for trigger event: '{}' to kafka topic '{}'",
        event.getId(), event.getTriggerEventId(), topicName, t));
  }

  private Future<Void> send(KafkaProducerRecord<String, String> kafkaProducerRecord, String topicName, int attempt) {
    var vertx = Vertx.currentContext().owner();
    var producerManager = new SimpleKafkaProducerManager(vertx, kafkaConfig);
    KafkaProducer<String, String> producer = producerManager.createShared(topicName);
    return producer.send(kafkaProducerRecord)
      .onComplete(reply -> producer.end().onComplete(v -> producer.close()))
      .<Void>mapEmpty()
      .recover(t -> {
        if (attempt >= MAX_SEND_ATTEMPTS) {
          return Future.failedFuture(t);
        }
        log.warn("send:: Attempt {} of {} to send to kafka topic '{}' failed, retrying", attempt, MAX_SEND_ATTEMPTS, topicName, t);
        return vertx.timer(RETRY_DELAY_MS).compose(v -> send(kafkaProducerRecord, topicName, attempt + 1));
      });
  }

}
