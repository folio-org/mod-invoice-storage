package org.folio.rest.impl;

import javax.ws.rs.core.Response;
import java.util.Map;

import org.folio.service.audit.AuditOutboxService;
import org.folio.rest.jaxrs.resource.InvoiceStorageAuditOutbox;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class AuditOutboxAPI implements InvoiceStorageAuditOutbox {

  @Autowired
  private AuditOutboxService auditOutboxService;

  public AuditOutboxAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void postInvoiceStorageAuditOutboxProcess(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    auditOutboxService.processOutboxEventLogs(okapiHeaders, vertxContext)
      .onSuccess(res -> asyncResultHandler.handle(Future.succeededFuture(Response.ok().build())))
      .onFailure(cause -> asyncResultHandler.handle(Future.failedFuture(cause)));
  }
}
