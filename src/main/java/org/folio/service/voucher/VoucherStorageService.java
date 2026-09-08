package org.folio.service.voucher;

import static org.folio.rest.impl.VoucherStorageImpl.VOUCHER_PREFIX;
import static org.folio.rest.utils.ResponseUtils.buildBadRequestResponse;
import static org.folio.rest.utils.ResponseUtils.buildErrorResponse;
import static org.folio.rest.utils.ResponseUtils.buildNoContentResponse;
import static org.folio.rest.utils.ResponseUtils.buildResponseWithLocation;
import static org.folio.rest.utils.RestConstants.OKAPI_URL;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.dao.voucher.VoucherDAO;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.jaxrs.model.VoucherAuditEvent;
import org.folio.rest.persist.DBClient;
import org.folio.service.audit.AuditOutboxService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class VoucherStorageService {

  private final VoucherDAO voucherDAO;
  private final AuditOutboxService auditOutboxService;

  public void createVoucher(Voucher voucher, Handler<AsyncResult<Response>> asyncResultHandler,
                            Context vertxContext, Map<String, String> headers) {
    log.info("createVoucher:: Creating a new voucher by id: {}", voucher.getId());
    new DBClient(vertxContext, headers).getPgClient()
      .withTrans(conn -> voucherDAO.createVoucher(voucher, conn)
        .compose(voucherId -> auditOutboxService.saveVoucherOutboxLog(conn, voucher, VoucherAuditEvent.Action.CREATE, headers)))
      .onSuccess(s -> {
        log.info("createVoucher:: Successfully created a new voucher by id: {}", voucher.getId());
        auditOutboxService.processOutboxEventLogs(headers, vertxContext);
        asyncResultHandler.handle(buildResponseWithLocation(headers.get(OKAPI_URL), VOUCHER_PREFIX + voucher.getId(), voucher));
      })
      .onFailure(f -> {
        log.error("Error occurred while creating a new voucher with id: {}", voucher.getId(), f);
        asyncResultHandler.handle(buildErrorResponse(f));
      });
  }

  public void updateVoucher(String id, Voucher voucher, Map<String, String> headers,
                            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.info("updateVoucher:: Updating voucher with id: {}", id);
    if (StringUtils.isBlank(id)) {
      asyncResultHandler.handle(buildBadRequestResponse("Voucher id is required"));
      return;
    }
    new DBClient(vertxContext, headers).getPgClient()
      .withTrans(conn -> voucherDAO.getVoucherByIdForUpdate(id, conn)
        .compose(original -> voucherDAO.updateVoucher(id, voucher, conn)
          .compose(v -> auditOutboxService.saveVoucherOutboxLog(conn, voucher, original, VoucherAuditEvent.Action.EDIT, headers))))
      .onSuccess(s -> {
        log.info("updateVoucher:: Successfully updated voucher with id: {}", id);
        auditOutboxService.processOutboxEventLogs(headers, vertxContext);
        asyncResultHandler.handle(buildNoContentResponse());
      })
      .onFailure(f -> {
        log.error("Error occurred while updating voucher with id: {}", id, f);
        asyncResultHandler.handle(buildErrorResponse(f));
      });
  }

}
