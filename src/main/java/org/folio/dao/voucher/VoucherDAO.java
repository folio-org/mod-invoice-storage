package org.folio.dao.voucher;

import org.folio.rest.jaxrs.model.Voucher;
import org.folio.rest.persist.Conn;

import io.vertx.core.Future;

public interface VoucherDAO {

  Future<Voucher> getVoucherByIdForUpdate(String voucherId, Conn conn);
  Future<String> createVoucher(Voucher voucher, Conn conn);
  Future<Void> updateVoucher(String id, Voucher voucher, Conn conn);

}
