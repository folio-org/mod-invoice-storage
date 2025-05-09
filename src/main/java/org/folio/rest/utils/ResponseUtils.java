package org.folio.rest.utils;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.Tx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.HttpException;

public class ResponseUtils {

  private static final Logger log = LogManager.getLogger(ResponseUtils.class);

  private ResponseUtils() {
  }

  public static <T> Handler<AsyncResult<Tx<T>>> handleNoContentResponse(Handler<AsyncResult<Response>> asyncResultHandler, Tx<T> tx,
    String logMessage) {
    return result -> {
      if (result.failed()) {
        HttpException cause = (HttpException) result.cause();
        log.error(logMessage, cause, tx.getEntity(), "or associated data failed to be");

        // The result of rollback operation is not so important, main failure cause is used to build the response
        tx.rollbackTransaction().onComplete(res -> asyncResultHandler.handle(buildErrorResponse(cause)));
      } else {
        log.info(logMessage, tx.getEntity(), "and associated data were successfully");
        asyncResultHandler.handle(buildNoContentResponse());
      }
    };
  }

  public static void handleFailure(Promise<?> promise, AsyncResult<?> reply) {
    promise.fail(convertPgExceptionIfNeeded(reply.cause()));
  }

  public static Throwable convertPgExceptionIfNeeded(Throwable cause) {
    var badRequestMessage = PgExceptionUtil.badRequestMessage(cause);
    if (badRequestMessage != null) {
      return new HttpException(BAD_REQUEST.getStatusCode(), badRequestMessage);
    } else {
      return new HttpException(INTERNAL_SERVER_ERROR.getStatusCode(), cause.getMessage());
    }
  }

  public static Future<Response> buildResponseWithLocation(String okapi, String endpoint, Object body) {
    try {
      return Future.succeededFuture(
        Response.created(new URI(okapi + endpoint))
        .header(CONTENT_TYPE, APPLICATION_JSON)
        .entity(body)
        .build()
      );
    } catch (URISyntaxException e) {
      return Future.succeededFuture(
        Response.created(URI.create(endpoint))
        .header(CONTENT_TYPE, APPLICATION_JSON)
        .header(LOCATION, endpoint)
        .entity(body)
        .build()
      );
    }
  }

  public static Future<Response> buildNoContentResponse() {
    return Future.succeededFuture(Response.noContent().build());
  }

  public static Future<Response> buildOkResponse(Object body) {
    return Future.succeededFuture(Response.ok(body, APPLICATION_JSON).build());
  }

  public static Future<Response> buildBadRequestResponse(String body) {
    return Future.succeededFuture(buildErrorResponse(BAD_REQUEST.getStatusCode(), body));
  }

  public static Future<Response> buildErrorResponse(Throwable throwable) {
    final String message;
    final int code;

    if (throwable instanceof HttpException httpException) {
      code = httpException.getStatusCode();
      message = httpException.getPayload();
    } else {
      code = INTERNAL_SERVER_ERROR.getStatusCode();
      message = throwable.getMessage();
    }

    return Future.succeededFuture(buildErrorResponse(code, message));
  }

  private static Response buildErrorResponse(int code, String message) {
    return Response.status(code)
      .header(CONTENT_TYPE, TEXT_PLAIN)
      .entity(message)
      .build();
  }

}
