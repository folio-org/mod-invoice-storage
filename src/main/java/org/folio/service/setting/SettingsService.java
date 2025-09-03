package org.folio.service.setting;

import javax.ws.rs.core.Response;
import java.util.Map;

import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.model.SettingCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorageSettings;
import org.folio.rest.persist.PgUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import lombok.extern.log4j.Log4j2;

/**
 * This service class is used to fetch and manage internal settings in the invoice storage module
 */
@Log4j2
public class SettingsService {

  private static final String SETTINGS_TABLE = "settings";

  public void getSettings(String query, int offset, int limit, Map<String, String> okapiHeaders,
                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(SETTINGS_TABLE, Setting.class, SettingCollection.class, query, offset, limit, okapiHeaders, vertxContext,
      InvoiceStorageSettings.GetInvoiceStorageSettingsResponse.class, asyncResultHandler);
  }

  public void createSetting(Setting entity, Map<String, String> okapiHeaders,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(SETTINGS_TABLE, entity, okapiHeaders, vertxContext,
      InvoiceStorageSettings.PostInvoiceStorageSettingsResponse.class, asyncResultHandler);
  }

  public void getSettingById(String id, Map<String, String> okapiHeaders,
                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(SETTINGS_TABLE, Setting.class, id, okapiHeaders, vertxContext,
      InvoiceStorageSettings.GetInvoiceStorageSettingsByIdResponse.class, asyncResultHandler);
  }

  public void updateSetting(String id, Setting entity, Map<String, String> okapiHeaders,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(SETTINGS_TABLE, entity, id, okapiHeaders, vertxContext,
      InvoiceStorageSettings.PutInvoiceStorageSettingsByIdResponse.class, asyncResultHandler);
  }

  public void deleteSetting(String id, Map<String, String> okapiHeaders,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(SETTINGS_TABLE, id, okapiHeaders, vertxContext,
      InvoiceStorageSettings.PutInvoiceStorageSettingsByIdResponse.class, asyncResultHandler);
  }

}
