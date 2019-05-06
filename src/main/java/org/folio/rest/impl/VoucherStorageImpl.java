package org.folio.rest.impl;

import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorage.PutInvoiceStorageInvoicesByIdResponse;
import org.folio.rest.jaxrs.resource.VoucherStorage;
import org.folio.rest.persist.EntitiesMetadataHolder;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.QueryHolder;

public class VoucherStorageImpl implements VoucherStorage {

  private PostgresClient pgClient;

  public static final String VOUCHER_TABLE = "vouchers";
  private static final String ID_FIELD_NAME = "id";

  public VoucherStorageImpl(Vertx vertx, String tenantId) {
    pgClient = PostgresClient.getInstance(vertx, tenantId);
    pgClient.setIdField(ID_FIELD_NAME);
  }

  @Validate
  @Override
  public void getVoucherStorageVouchers(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    EntitiesMetadataHolder<Voucher, VoucherCollection> entitiesMetadataHolder = new EntitiesMetadataHolder<>(Voucher.class,
        VoucherCollection.class, GetVoucherStorageVouchersResponse.class);
    QueryHolder cql = new QueryHolder(VOUCHER_TABLE, query, offset, limit, lang);
    getEntitiesCollection(entitiesMetadataHolder, cql, asyncResultHandler, vertxContext, okapiHeaders);
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
}
