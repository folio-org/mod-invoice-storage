package org.folio.service.adjustment;

import javax.ws.rs.core.Response;
import java.util.Map;

import org.folio.rest.jaxrs.model.AdjustmentPreset;
import org.folio.rest.jaxrs.model.AdjustmentPresetCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorageAdjustmentPresets;
import org.folio.rest.persist.PgUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AdjustmentPresetsService {

  private static final String ADJUSTMENT_PRESETS_TABLE = "adjustment_presets";

  public void getAdjustmentPresets(String query, int offset, int limit, Map<String, String> okapiHeaders,
                          Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(ADJUSTMENT_PRESETS_TABLE, AdjustmentPreset.class, AdjustmentPresetCollection.class, query, offset, limit, okapiHeaders, vertxContext,
      InvoiceStorageAdjustmentPresets.GetInvoiceStorageAdjustmentPresetsResponse.class, asyncResultHandler);
  }

  public void createAdjustmentPreset(AdjustmentPreset entity, Map<String, String> okapiHeaders,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(ADJUSTMENT_PRESETS_TABLE, entity, okapiHeaders, vertxContext,
      InvoiceStorageAdjustmentPresets.PostInvoiceStorageAdjustmentPresetsResponse.class, asyncResultHandler);
  }

  public void getAdjustmentPresetById(String id, Map<String, String> okapiHeaders,
                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(ADJUSTMENT_PRESETS_TABLE, AdjustmentPreset.class, id, okapiHeaders, vertxContext,
      InvoiceStorageAdjustmentPresets.GetInvoiceStorageAdjustmentPresetsByIdResponse.class, asyncResultHandler);
  }

  public void updateAdjustmentPreset(String id, AdjustmentPreset entity, Map<String, String> okapiHeaders,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(ADJUSTMENT_PRESETS_TABLE, entity, id, okapiHeaders, vertxContext,
      InvoiceStorageAdjustmentPresets.PutInvoiceStorageAdjustmentPresetsByIdResponse.class, asyncResultHandler);
  }

  public void deleteAdjustmentPreset(String id, Map<String, String> okapiHeaders,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(ADJUSTMENT_PRESETS_TABLE, id, okapiHeaders, vertxContext,
      InvoiceStorageAdjustmentPresets.PutInvoiceStorageAdjustmentPresetsByIdResponse.class, asyncResultHandler);
  }

}
