package org.folio.rest.utils;

public class TestData {

  public interface Invoice {
    String DEFAULT = "data/invoices/123invoicenumber45_approved_for_2_orders.json";
  }

  public interface InvoiceLines {
    String DEFAULT = "data/invoice-lines/123invoicenumber45-1.json";
  }

  public interface Voucher {
    String DEFAULT = "data/vouchers/test_voucher.json";
  }

  public interface VoucherLines {
    String DEFAULT = "data/voucher-lines/test_voucher_line.json";
  }

  public interface BatchGroup {
    String DEFAULT = "data/batch-groups/test-batch-group.json";
  }

  public interface BatchVoucherExportConfigs {
    String DEFAULT = "data/batch-voucher-export-configs/test_config.json";
  }

  public interface BatchVoucher {
    String DEFAULT = "data/batch-voucher-exports/batch-vouchers/test-batch-voucher.json";
  }

  public interface BatchVoucherExports {
    String DEFAULT = "data/batch-voucher-exports/test-batch-voucher-export.json";
  }

  public interface Settings {
    String DEFAULT = "data/settings/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d.json";
  }

}
