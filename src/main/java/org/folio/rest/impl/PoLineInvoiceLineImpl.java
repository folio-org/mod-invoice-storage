package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.model.Line;
import org.folio.rest.jaxrs.resource.PoLineInvoiceLine;

import javax.ws.rs.core.Response;
import java.util.Map;

public class PoLineInvoiceLineImpl implements PoLineInvoiceLine {

  @Override
  public void getPoLineInvoiceLineLines(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void postPoLineInvoiceLineLines(String lang, Line entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void getPoLineInvoiceLineLinesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void deletePoLineInvoiceLineLinesById(String id, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }

  @Override
  public void putPoLineInvoiceLineLinesById(String id, String lang, Line entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

  }
}
