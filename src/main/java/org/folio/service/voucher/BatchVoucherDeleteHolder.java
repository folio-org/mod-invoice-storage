package org.folio.service.voucher;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Data
public class BatchVoucherDeleteHolder {

  private String batchVoucherId;
  private String batchVoucherExportId;

}
