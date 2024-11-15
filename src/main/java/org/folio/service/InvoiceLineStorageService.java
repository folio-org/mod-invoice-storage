package org.folio.service;

import static org.folio.rest.impl.InvoiceStorageImpl.INVOICE_LINES_PREFIX;
import static org.folio.rest.utils.ResponseUtils.buildBadRequestResponse;
import static org.folio.rest.utils.ResponseUtils.buildErrorResponse;
import static org.folio.rest.utils.ResponseUtils.buildNoContentResponse;
import static org.folio.rest.utils.ResponseUtils.buildResponseWithLocation;
import static org.folio.rest.utils.RestConstants.OKAPI_URL;

import javax.ws.rs.core.Response;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.dao.lines.InvoiceLinesDAO;
import org.folio.rest.jaxrs.model.InvoiceLine;
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.persist.DBClient;
import org.folio.service.audit.AuditOutboxService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class InvoiceLineStorageService {

  private final InvoiceLinesDAO invoiceLinesDAO;
  private final AuditOutboxService auditOutboxService;

  public void createInvoiceLine(InvoiceLine invoiceLine, Handler<AsyncResult<Response>> asyncResultHandler,
                                Context vertxContext, Map<String, String> headers) {
    log.info("createInvoiceLine:: Creating a new invoiceLine by id: {}", invoiceLine.getId());
    new DBClient(vertxContext, headers).getPgClient()
      .withTrans(conn -> invoiceLinesDAO.createInvoiceLine(invoiceLine, conn)
        .map(invoiceLine::withId)
        .compose(invoiceLineId -> auditOutboxService.saveInvoiceLineOutboxLog(conn, invoiceLine, InvoiceLineAuditEvent.Action.CREATE, headers)))
      .onSuccess(s -> {
        log.info("createInvoiceLine:: Successfully created a new invoiceLine by id: {}", invoiceLine.getId());
        auditOutboxService.processOutboxEventLogs(headers, vertxContext);
        asyncResultHandler.handle(buildResponseWithLocation(headers.get(OKAPI_URL), INVOICE_LINES_PREFIX + invoiceLine.getId(), invoiceLine));
      })
      .onFailure(f -> {
        log.error("Error occurred while creating a new invoiceLine with id: {}", invoiceLine.getId(), f);
        asyncResultHandler.handle(buildErrorResponse(f));
      });
  }

  public void updateInvoiceLine(String id, InvoiceLine invoiceLine, Map<String, String> headers,
                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    log.info("updateInvoiceLine:: Updating invoice line with id: {}", id);
    if (StringUtils.isBlank(id)) {
      asyncResultHandler.handle(buildBadRequestResponse("Invoice line id is required"));
    }
    new DBClient(vertxContext, headers).getPgClient()
      .withTrans(conn -> invoiceLinesDAO.updateInvoiceLine(id, invoiceLine, conn)
        .compose(invoiceLineId -> auditOutboxService.saveInvoiceLineOutboxLog(conn, invoiceLine, InvoiceLineAuditEvent.Action.EDIT, headers)))
      .onSuccess(s -> {
        log.info("updateInvoiceLine:: Successfully updated invoice line with id: {}", id);
        auditOutboxService.processOutboxEventLogs(headers, vertxContext);
        asyncResultHandler.handle(buildNoContentResponse());
      })
      .onFailure(f -> {
        log.error("Error occurred while updating invoice line with id: {}", id, f);
        asyncResultHandler.handle(buildErrorResponse(f));
      });
  }

}
