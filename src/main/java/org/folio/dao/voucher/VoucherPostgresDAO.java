package org.folio.dao.voucher;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.folio.rest.impl.VoucherStorageImpl.VOUCHER_TABLE;
import static org.folio.rest.utils.ResponseUtils.convertPgExceptionIfNeeded;

import java.util.UUID;

import org.folio.dao.DbUtils;
import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.persist.Conn;

import io.vertx.core.Future;
import io.vertx.ext.web.handler.HttpException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VoucherPostgresDAO implements VoucherDAO {

  @Override
  public Future<Voucher> getVoucherByIdForUpdate(String voucherId, Conn conn) {
    return conn.getByIdForUpdate(VOUCHER_TABLE, voucherId, Voucher.class)
      .map(voucher -> {
        if (voucher == null) {
          throw new HttpException(NOT_FOUND.getStatusCode(), NOT_FOUND.getReasonPhrase());
        }
        return voucher;
      })
      .onFailure(t -> log.error("getVoucherByIdForUpdate failed for voucher with id {}", voucherId, t));
  }

  @Override
  public Future<String> createVoucher(Voucher voucher, Conn conn) {
    log.info("Creating new voucher with id={}", voucher.getId());
    if (voucher.getId() == null) {
      voucher.setId(UUID.randomUUID().toString());
    }
    return conn.save(VOUCHER_TABLE, voucher.getId(), voucher, true)
      .recover(t -> Future.failedFuture(convertPgExceptionIfNeeded(t)))
      .onSuccess(s -> log.info("createVoucher:: New voucher with id: '{}' successfully created", voucher.getId()))
      .onFailure(t -> log.error("Failed to create voucher with id: '{}'", voucher.getId(), t));
  }

  @Override
  public Future<Void> updateVoucher(String id, Voucher voucher, Conn conn) {
    return conn.update(VOUCHER_TABLE, voucher, id)
      .compose(DbUtils::verifyEntityUpdate)
      .onSuccess(v -> log.info("updateVoucher:: Voucher with id: '{}' successfully updated", id))
      .onFailure(t -> log.error("Update failed for voucher with id: '{}'", id, t))
      .mapEmpty();
  }

}
