package org.folio.rest.utils;

public class TestData {

  public interface Invoice {
    String DEFAULT = "mockdata/invoices/123invoicenumber45_approved_for_2_orders.json";
  }

  public interface InvoiceLines {
    String DEFAULT = "mockdata/invoice-lines/123invoicenumber45-1.json";
  }

  public interface Voucher {
    String DEFAULT = "mockdata/vouchers/test_voucher.json";
  }

  public interface VoucherLines {
    String DEFAULT = "mockdata/voucher-lines/test_voucher_line.json";
  }

  public interface BatchGroup {
    String DEFAULT = "mockdata/batch-groups/test-batch-group.json";
  }

  public interface BatchVoucherExportConfigs {
    String DEFAULT = "mockdata/batch-voucher-export-configs/test_config.json";
  }

  public interface BatchVoucher {
    String DEFAULT = "mockdata/batch-voucher-exports/batch-vouchers/test-batch-voucher.json";
  }

  public interface BatchVoucherExports {
    String DEFAULT = "mockdata/batch-voucher-exports/test-batch-voucher-export.json";
  }

  public interface Settings {
    String DEFAULT = "mockdata/settings/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d.json";
  }

}
