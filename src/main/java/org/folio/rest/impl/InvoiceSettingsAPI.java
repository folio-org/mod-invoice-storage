package org.folio.rest.impl;

import javax.ws.rs.core.Response;
import java.util.Map;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.resource.InvoiceStorageSettings;
import org.folio.service.setting.SettingsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class InvoiceSettingsAPI implements InvoiceStorageSettings {

  @Autowired
  private SettingsService settingsService;

  public InvoiceSettingsAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  public void getInvoiceStorageSettings(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
                                       Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.getSettings(query, offset, limit, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void postInvoiceStorageSettings(Setting entity, Map<String, String> okapiHeaders,
                                        Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.createSetting(entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void getInvoiceStorageSettingsById(String id, Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.getSettingById(id, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void putInvoiceStorageSettingsById(String id, Setting entity, Map<String, String> okapiHeaders,
                                           Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.updateSetting(id, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  @Validate
  public void deleteInvoiceStorageSettingsById(String id, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    settingsService.deleteSetting(id, okapiHeaders, asyncResultHandler, vertxContext);
  }

}

