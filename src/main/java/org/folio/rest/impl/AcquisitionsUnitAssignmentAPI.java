package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.AcquisitionsUnitAssignment;
import org.folio.rest.jaxrs.model.AcquisitionsUnitAssignmentCollection;
import org.folio.rest.jaxrs.resource.InvoiceStorageAcquisitionsUnitAssignments;
import org.folio.rest.jaxrs.resource.VoucherStorageAcquisitionsUnitAssignments;
import org.folio.rest.persist.PgUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class AcquisitionsUnitAssignmentAPI implements InvoiceStorageAcquisitionsUnitAssignments, VoucherStorageAcquisitionsUnitAssignments {
  private static final String ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE = "acquisitions_unit_assignments";
  private static final String VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE = "voucher_acquisitions_unit_assignments";

  
  @Override
  @Validate
  public void postInvoiceStorageAcquisitionsUnitAssignments(String lang, AcquisitionsUnitAssignment entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, entity, okapiHeaders, vertxContext, PostInvoiceStorageAcquisitionsUnitAssignmentsResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void getInvoiceStorageAcquisitionsUnitAssignments(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, AcquisitionsUnitAssignment.class, AcquisitionsUnitAssignmentCollection.class,
      query, offset, limit, okapiHeaders, vertxContext, GetInvoiceStorageAcquisitionsUnitAssignmentsResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void putInvoiceStorageAcquisitionsUnitAssignmentsById(String id, String lang, AcquisitionsUnitAssignment entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, entity, id, okapiHeaders, vertxContext, PutInvoiceStorageAcquisitionsUnitAssignmentsByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void getInvoiceStorageAcquisitionsUnitAssignmentsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, AcquisitionsUnitAssignment.class, id, okapiHeaders, vertxContext, GetInvoiceStorageAcquisitionsUnitAssignmentsByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteInvoiceStorageAcquisitionsUnitAssignmentsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, id, okapiHeaders, vertxContext, DeleteInvoiceStorageAcquisitionsUnitAssignmentsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postVoucherStorageAcquisitionsUnitAssignments(String lang, AcquisitionsUnitAssignment entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, entity, okapiHeaders, vertxContext, PostVoucherStorageAcquisitionsUnitAssignmentsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getVoucherStorageAcquisitionsUnitAssignments(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, AcquisitionsUnitAssignment.class, AcquisitionsUnitAssignmentCollection.class,
      query, offset, limit, okapiHeaders, vertxContext, GetVoucherStorageAcquisitionsUnitAssignmentsResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putVoucherStorageAcquisitionsUnitAssignmentsById(String id, String lang, AcquisitionsUnitAssignment entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, entity, id, okapiHeaders, vertxContext, PutVoucherStorageAcquisitionsUnitAssignmentsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void getVoucherStorageAcquisitionsUnitAssignmentsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, AcquisitionsUnitAssignment.class, id, okapiHeaders, vertxContext, GetVoucherStorageAcquisitionsUnitAssignmentsByIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteVoucherStorageAcquisitionsUnitAssignmentsById(String id, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(VOUCHER_ACQUISITIONS_UNIT_ASSIGNMENTS_TABLE, id, okapiHeaders, vertxContext, DeleteVoucherStorageAcquisitionsUnitAssignmentsByIdResponse.class, asyncResultHandler);
  }
}
