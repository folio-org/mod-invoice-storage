package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.Vertx;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.resource.InvoiceStorageInvoiceLineNumber;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.service.InvoiceLineNumberService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class InvoiceLineNumberAPI implements InvoiceStorageInvoiceLineNumber {

  @Autowired
  private InvoiceLineNumberService invoiceLineNumberService;

  public InvoiceLineNumberAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Validate
  @Override
  public void getInvoiceStorageInvoiceLineNumber(String invoiceId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    invoiceLineNumberService.getInvoiceStorageInvoiceLineNumber(invoiceId, okapiHeaders, asyncResultHandler, vertxContext);
	}
}
