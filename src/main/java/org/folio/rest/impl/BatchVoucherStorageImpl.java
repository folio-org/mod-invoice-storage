package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Credentials;
import org.folio.rest.jaxrs.model.ExportConfig;
import org.folio.rest.jaxrs.model.ExportConfigCollection;
import org.folio.rest.jaxrs.resource.BatchVoucherStorage;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BatchVoucherStorageImpl implements BatchVoucherStorage {

  public static final Logger logger = LoggerFactory.getLogger(BatchVoucherStorageImpl.class);

  public static final String BATCH_VOUCHER_EXPORT_CONFIGS_TABLE = "batch_voucher_export_configs";
  public static final String EXPORT_CONFIG_CREDENTIALS_TABLE = "export_config_credentials";
  private static final String MISMATCH_ERROR_MESSAGE = "Batch voucher export configuration credentials id mismatch";

  @Validate
  @Override
  public void getBatchVoucherStorageExportConfigurations(int offset, int limit, String query, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, ExportConfig.class, ExportConfigCollection.class, query, offset, limit,
        okapiHeaders, vertxContext, BatchVoucherStorage.GetBatchVoucherStorageExportConfigurationsResponse.class,
        asyncResultHandler);
  }

  @Validate
  @Override
  public void postBatchVoucherStorageExportConfigurations(String lang, ExportConfig entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, entity, okapiHeaders, vertxContext,
        BatchVoucherStorage.PostBatchVoucherStorageExportConfigurationsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getBatchVoucherStorageExportConfigurationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, ExportConfig.class, id, okapiHeaders, vertxContext,
        BatchVoucherStorage.GetBatchVoucherStorageExportConfigurationsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteBatchVoucherStorageExportConfigurationsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, id, okapiHeaders, vertxContext,
        BatchVoucherStorage.DeleteBatchVoucherStorageExportConfigurationsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putBatchVoucherStorageExportConfigurationsById(String id, String lang, ExportConfig entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(BATCH_VOUCHER_EXPORT_CONFIGS_TABLE, entity, id, okapiHeaders, vertxContext,
        BatchVoucherStorage.PutBatchVoucherStorageExportConfigurationsByIdResponse.class, asyncResultHandler);

  }

  @Validate
  @Override
  public void postBatchVoucherStorageExportConfigurationsCredentialsById(String id, Credentials entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (StringUtils.equals(entity.getExportConfigId(), id)) {
      PgUtil.post(EXPORT_CONFIG_CREDENTIALS_TABLE, entity, okapiHeaders, vertxContext,
          PostBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.class, asyncResultHandler);
    } else {
      asyncResultHandler.handle(io.vertx.core.Future
        .succeededFuture(PostBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.respond400WithTextPlain(MISMATCH_ERROR_MESSAGE)));
    }
  }

  @Validate
  @Override
  public void getBatchVoucherStorageExportConfigurationsCredentialsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      try {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient pgClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);
        CQLWrapper cqlWrapper = getCqlWrapperByExportConfigId(id);

        pgClient.get(EXPORT_CONFIG_CREDENTIALS_TABLE, Credentials.class, cqlWrapper, false, reply -> {
          try {
            if (reply.succeeded()) {
              if (reply.result()
                .getResults()
                .isEmpty()) {
                asyncResultHandler.handle(Future.succeededFuture(GetBatchVoucherStorageExportConfigurationsCredentialsByIdResponse
                  .respond404WithTextPlain(Response.Status.NOT_FOUND.getReasonPhrase())));
              } else {
                Credentials response = reply.result()
                  .getResults()
                  .get(0);
                asyncResultHandler.handle(Future.succeededFuture(
                    GetBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.respond200WithApplicationJson(response)));
              }
            } else {
              asyncResultHandler.handle(Future.succeededFuture(
                  GetBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.respond500WithTextPlain(reply.cause()
                    .getMessage())));
            }
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(GetBatchVoucherStorageExportConfigurationsCredentialsByIdResponse
              .respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
          }
        });
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        asyncResultHandler.handle(Future.succeededFuture(GetBatchVoucherStorageExportConfigurationsCredentialsByIdResponse
          .respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
      }
    });
  }

  @Validate
  @Override
  public void deleteBatchVoucherStorageExportConfigurationsCredentialsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    vertxContext.runOnContext((Void v) -> {
      try {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));
        PostgresClient pgClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);
        CQLWrapper cqlWrapper = getCqlWrapperByExportConfigId(id);

        pgClient.delete(EXPORT_CONFIG_CREDENTIALS_TABLE, cqlWrapper, reply -> {
          try {
            if (reply.succeeded()) {
              if (reply.result()
                .getUpdated() == 0) {
                asyncResultHandler
                  .handle(Future.succeededFuture(DeleteBatchVoucherStorageExportConfigurationsCredentialsByIdResponse
                    .respond404WithTextPlain(Response.Status.NOT_FOUND.getReasonPhrase())));
              } else {
                asyncResultHandler.handle(
                    Future.succeededFuture(DeleteBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.respond204()));
              }
            } else {
              asyncResultHandler.handle(Future.succeededFuture(
                  DeleteBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.respond500WithTextPlain(reply.cause()
                    .getMessage())));
            }
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(DeleteBatchVoucherStorageExportConfigurationsCredentialsByIdResponse
              .respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
          }
        });
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        asyncResultHandler.handle(Future.succeededFuture(DeleteBatchVoucherStorageExportConfigurationsCredentialsByIdResponse
          .respond500WithTextPlain(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())));
      }
    });
  }

  @Validate
  @Override
  public void putBatchVoucherStorageExportConfigurationsCredentialsById(String id, String lang, Credentials entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (StringUtils.equals(entity.getExportConfigId(), id)) {
      PgUtil.put(EXPORT_CONFIG_CREDENTIALS_TABLE, entity, entity.getId(), okapiHeaders, vertxContext,
          BatchVoucherStorage.PutBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.class, asyncResultHandler);
    } else {
      asyncResultHandler.handle(io.vertx.core.Future
        .succeededFuture(PutBatchVoucherStorageExportConfigurationsCredentialsByIdResponse.respond400WithTextPlain(MISMATCH_ERROR_MESSAGE)));
    }
  }

  private CQLWrapper getCqlWrapperByExportConfigId(String exportConfigId) {
    Criteria criteria = new Criteria();
    criteria.addField("'exportConfigId'");
    criteria.setOperation("=");
    criteria.setVal(exportConfigId);
    return new CQLWrapper(new Criterion(criteria));
  }
}
