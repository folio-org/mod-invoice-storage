package org.folio.rest.impl;

import static org.folio.rest.persist.HelperUtils.ID_FIELD_NAME;
import static org.folio.rest.persist.HelperUtils.getEntitiesCollectionWithDistinctOn;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherCollection;
import org.folio.rest.jaxrs.model.VoucherLine;
import org.folio.rest.jaxrs.model.VoucherLineCollection;
import org.folio.rest.jaxrs.resource.VoucherStorage;
import org.folio.rest.persist.EntitiesMetadataHolder;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.QueryHolder;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class VoucherStorageImpl implements VoucherStorage {

  public static final String ACQUISITIONS_UNIT_ASSIGNMENTS = "acquisitionsUnitAssignments.";
  public static final String VOUCHER_TABLE = "vouchers";
  public static final String VOUCHER_LINE_TABLE = "voucher_lines";
  public static final String VOUCHERS_VIEW = "vouchers_view";
  private static final String VOUCHER_LINES_VIEW = "voucher_lines_view";

  @Validate
  @Override
  public void getVoucherStorageVouchers(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String tableName = getTableName(query, VOUCHER_TABLE, VOUCHERS_VIEW);
    EntitiesMetadataHolder<Voucher, VoucherCollection> entitiesMetadataHolder = new EntitiesMetadataHolder<>(
      Voucher.class, VoucherCollection.class, GetVoucherStorageVouchersResponse.class);
    QueryHolder cql = new QueryHolder(tableName, query, offset, limit, lang);
    getEntitiesCollectionWithDistinctOn(entitiesMetadataHolder, cql, ID_FIELD_NAME, asyncResultHandler, vertxContext, okapiHeaders);
  }

  private String getTableName(String cql, String table, String view) {
    if(cql != null && cql.contains(ACQUISITIONS_UNIT_ASSIGNMENTS)){
      return view;
    }
    return table;
  }

  @Validate
  @Override
  public void postVoucherStorageVouchers(String lang, Voucher entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(VOUCHER_TABLE, entity, okapiHeaders, vertxContext, PostVoucherStorageVouchersResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getVoucherStorageVouchersById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(VOUCHER_TABLE, Voucher.class, id, okapiHeaders, vertxContext, GetVoucherStorageVouchersByIdResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteVoucherStorageVouchersById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(VOUCHER_TABLE, id, okapiHeaders, vertxContext, DeleteVoucherStorageVouchersByIdResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void putVoucherStorageVouchersById(String id, String lang, Voucher entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(VOUCHER_TABLE, entity, id, okapiHeaders, vertxContext, PutVoucherStorageVouchersByIdResponse.class,
        asyncResultHandler);

  }

  @Override
  public void getVoucherStorageVoucherLines(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      String tableName = getTableName(query, VOUCHER_LINE_TABLE, VOUCHER_LINES_VIEW);
      EntitiesMetadataHolder<VoucherLine, VoucherLineCollection> entitiesMetadataHolder = new EntitiesMetadataHolder<>(
          VoucherLine.class, VoucherLineCollection.class, GetVoucherStorageVoucherLinesResponse.class);
      QueryHolder cql = new QueryHolder(tableName, query, offset, limit, lang);
      getEntitiesCollectionWithDistinctOn(entitiesMetadataHolder, cql, ID_FIELD_NAME, asyncResultHandler, vertxContext, okapiHeaders);
    });
  }

  @Override
  public void postVoucherStorageVoucherLines(String lang, VoucherLine entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(VOUCHER_LINE_TABLE, entity, okapiHeaders, vertxContext, PostVoucherStorageVoucherLinesResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putVoucherStorageVoucherLinesById(String id, String lang, VoucherLine entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(VOUCHER_LINE_TABLE, entity, id, okapiHeaders, vertxContext, PutVoucherStorageVoucherLinesByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getVoucherStorageVoucherLinesById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(VOUCHER_LINE_TABLE, VoucherLine.class, id, okapiHeaders, vertxContext,
        GetVoucherStorageVoucherLinesByIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteVoucherStorageVoucherLinesById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(VOUCHER_LINE_TABLE, id, okapiHeaders, vertxContext, DeleteVoucherStorageVoucherLinesByIdResponse.class,
        asyncResultHandler);
  }
}
