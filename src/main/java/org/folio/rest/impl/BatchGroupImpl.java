package org.folio.rest.impl;

import static org.folio.rest.persist.HelperUtils.getEntitiesCollection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.BatchGroup;
import org.folio.rest.jaxrs.model.BatchGroupCollection;
import org.folio.rest.jaxrs.resource.BatchGroupStorage;
import org.folio.rest.persist.PgUtil;

import javax.ws.rs.core.Response;
import java.util.Map;

public class BatchGroupImpl implements BatchGroupStorage {

  public static final String BATCH_GROUP_TABLE = "batch_groups";

  @Validate
  @Override
  public void getBatchGroupStorageBatchGroups(int offset, int limit, String query, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(BATCH_GROUP_TABLE,BatchGroup.class,BatchGroupCollection.class,query,offset,limit,okapiHeaders,vertxContext,
      BatchGroupStorage.GetBatchGroupStorageBatchGroupsResponse.class,asyncResultHandler);
  }

  @Validate
  @Override
  public void postBatchGroupStorageBatchGroups(String lang, BatchGroup entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(BATCH_GROUP_TABLE, entity, okapiHeaders, vertxContext,
      BatchGroupStorage.PostBatchGroupStorageBatchGroupsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getBatchGroupStorageBatchGroupsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(BATCH_GROUP_TABLE, BatchGroup.class, id, okapiHeaders, vertxContext,
      BatchGroupStorage.GetBatchGroupStorageBatchGroupsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteBatchGroupStorageBatchGroupsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(BATCH_GROUP_TABLE, id, okapiHeaders, vertxContext,
      BatchGroupStorage.DeleteBatchGroupStorageBatchGroupsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putBatchGroupStorageBatchGroupsById(String id, String lang, BatchGroup entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(BATCH_GROUP_TABLE, entity, id, okapiHeaders, vertxContext,
      BatchGroupStorage.PutBatchGroupStorageBatchGroupsByIdResponse.class, asyncResultHandler);
  }
}
