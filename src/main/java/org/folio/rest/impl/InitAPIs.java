package org.folio.rest.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.config.ApplicationConfig;
import org.folio.dbschema.ObjectMapperTool;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.spring.SpringContextUtil;
import org.folio.verticles.CalendarTriggerConsumerVerticle;
import org.springframework.context.support.AbstractApplicationContext;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.SerializationConfig;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;

/**
 * The class initializes vertx context adding spring context
 */
public class InitAPIs implements InitAPI {

  private static final Logger log = LogManager.getLogger(InitAPIs.class);

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> resultHandler) {
    vertx.executeBlocking(() -> {
      SerializationConfig serializationConfig = ObjectMapperTool.getMapper().getSerializationConfig();
      DeserializationConfig deserializationConfig = ObjectMapperTool.getMapper().getDeserializationConfig();

      DatabindCodec.mapper().setConfig(serializationConfig);
      DatabindCodec.mapper().setConfig(deserializationConfig);

      SpringContextUtil.init(vertx, context, ApplicationConfig.class);
      return true;
    }).compose(v -> deployCalendarTriggerConsumerVerticle(vertx, context))
      .onComplete(result -> {
        if (result.succeeded()) {
          log.info("APIs initialized successfully");
          resultHandler.handle(Future.succeededFuture(true));
        } else {
          log.error("Failure to init API", result.cause());
          resultHandler.handle(Future.failedFuture(result.cause()));
        }
      });
  }

  private Future<Boolean> deployCalendarTriggerConsumerVerticle(Vertx vertx, Context context) {
    AbstractApplicationContext springContext = context.get("springContext");
    return vertx.deployVerticle(() -> springContext.getBean(CalendarTriggerConsumerVerticle.class), new DeploymentOptions())
      .onSuccess(id -> log.info("CalendarTriggerConsumerVerticle deployed"))
      .map(true)
      .recover(t -> {
        log.error("Failed to deploy CalendarTriggerConsumerVerticle", t);
        return Future.succeededFuture(true);
      });
  }

}
