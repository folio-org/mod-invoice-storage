package org.folio.rest.impl;

import javax.ws.rs.core.Response;
import java.util.Map;

import org.folio.rest.jaxrs.model.AdjustmentPreset;
import org.folio.rest.jaxrs.resource.InvoiceStorageAdjustmentPresets;
import org.folio.service.adjustment.AdjustmentPresetsService;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class AdjustmentPresetsAPI implements InvoiceStorageAdjustmentPresets {

  @Autowired
  private AdjustmentPresetsService adjustmentPresetsService;

  public AdjustmentPresetsAPI() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  public void getInvoiceStorageAdjustmentPresets(String query, String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
                                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    adjustmentPresetsService.getAdjustmentPresets(query, offset, limit, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void getInvoiceStorageAdjustmentPresetsById(String id, Map<String, String> okapiHeaders,
                                                     Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    adjustmentPresetsService.getAdjustmentPresetById(id, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void postInvoiceStorageAdjustmentPresets(AdjustmentPreset entity, Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    adjustmentPresetsService.createAdjustmentPreset(entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void putInvoiceStorageAdjustmentPresetsById(String id, AdjustmentPreset entity, Map<String, String> okapiHeaders,
                                                     Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    adjustmentPresetsService.updateAdjustmentPreset(id, entity, okapiHeaders, asyncResultHandler, vertxContext);
  }

  @Override
  public void deleteInvoiceStorageAdjustmentPresetsById(String id, Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    adjustmentPresetsService.deleteAdjustmentPreset(id, okapiHeaders, asyncResultHandler, vertxContext);
  }

}

