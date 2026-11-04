package org.folio.verticles;

import static org.folio.service.calendar.CalendarTriggerService.MODULE_NAME;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.folio.kafka.GlobalLoadSensor;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.KafkaConsumerWrapper;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.SubscriptionDefinition;
import org.folio.rest.jaxrs.model.CalendarTriggerEvent;
import org.folio.rest.persist.DBClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.service.calendar.CalendarTriggerService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Consumes the calendar trigger events mod-notification-center publishes to {@code {env}.{tenant}.NOTIFICATION_CALENDAR_TRIGGER}
 * and processes the ones addressed to this module. All instances share one consumer group, so each event is processed once.
 * The wrapper commits the offset when the handler completes, whether it succeeded or failed; an event is redelivered
 * only when the module stops before the handler completes.
 */
@Log4j2
@RequiredArgsConstructor
public class CalendarTriggerConsumerVerticle extends AbstractVerticle {

  static final String CALENDAR_TRIGGER_TOPIC = "NOTIFICATION_CALENDAR_TRIGGER";
  static final String TARGET_MODULE_HEADER = "x-folio-target-module";
  private static final int LOAD_LIMIT = 5;

  private final KafkaConfig kafkaConfig;
  private final CalendarTriggerService calendarTriggerService;
  private KafkaConsumerWrapper<String, String> consumerWrapper;

  @Override
  public void start(Promise<Void> startPromise) {
    // mod-notification-center topics have no namespace segment: {env}.{tenant}.NOTIFICATION_CALENDAR_TRIGGER
    var subscriptionDefinition = SubscriptionDefinition.builder()
      .eventType(CALENDAR_TRIGGER_TOPIC)
      .subscriptionPattern(kafkaConfig.getEnvId() + "\\.\\w+\\." + CALENDAR_TRIGGER_TOPIC)
      .build();

    consumerWrapper = KafkaConsumerWrapper.<String, String>builder()
      .context(context)
      .vertx(vertx)
      .kafkaConfig(kafkaConfig)
      .loadLimit(LOAD_LIMIT)
      .globalLoadSensor(new GlobalLoadSensor())
      .subscriptionDefinition(subscriptionDefinition)
      .groupInstanceId(MODULE_NAME + "-" + getClass().getSimpleName() + "-" + UUID.randomUUID())
      .build();

    // module id in <artifactId>-<version> format, used when KAFKA_TENANT_FILTER_ENABLED is on
    consumerWrapper.start(this::handle, MODULE_NAME, MODULE_NAME + "-" + ModuleName.getModuleVersion())
      .onSuccess(v -> log.info("start:: CalendarTriggerConsumerVerticle started, subscription pattern: '{}'",
        subscriptionDefinition.getSubscriptionPattern()))
      .onFailure(t -> log.error("start:: Failed to start CalendarTriggerConsumerVerticle", t))
      .onComplete(startPromise);
  }

  Future<String> handle(KafkaConsumerRecord<String, String> kafkaRecord) {
    // case-insensitive: mod-notification-center sends lower-case header names, RMB reads e.g. X-Okapi-Tenant
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.putAll(KafkaHeaderUtils.kafkaHeadersToMap(kafkaRecord.headers()));
    var targetModule = headers.get(TARGET_MODULE_HEADER);
    if (!MODULE_NAME.equals(targetModule)) {
      log.debug("handle:: Skipping trigger event for module: '{}' from topic: '{}'", targetModule, kafkaRecord.topic());
      return Future.succeededFuture(kafkaRecord.key());
    }
    try {
      var event = Json.decodeValue(kafkaRecord.value(), CalendarTriggerEvent.class);
      return calendarTriggerService.processTrigger(event, new DBClient(context, headers), headers)
        .map(kafkaRecord.key());
    } catch (Exception e) {
      log.error("handle:: Failed to process trigger event from topic: '{}', offset: {}",
        kafkaRecord.topic(), kafkaRecord.offset(), e);
      return Future.failedFuture(e);
    }
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    consumerWrapper.stop().onComplete(stopPromise);
  }

}
