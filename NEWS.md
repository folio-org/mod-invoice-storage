## 1.1.0 - Unreleased

## 1.0.0 - Released

The primary focus of this release was to provide initial version of the API for managing invoices, invoice lines, voucher, voucher lines and acquisitions unit assignments.

### Stories
* [MODINVOSTO-26](https://issues.folio.org/browse/MODINVOSTO-26) - Implement basic CRUD for /invoice-storage/acquisitions-unit-assignments
* [MODINVOSTO-24](https://issues.folio.org/browse/MODINVOSTO-24) - Invoice schema: add billTo and exportToAccounting properties
* [MODINVOSTO-22](https://issues.folio.org/browse/MODINVOSTO-22) - Remove invoice.acquisitionsUnit
* [MODINVOSTO-19](https://issues.folio.org/browse/MODINVOSTO-19) - Remove associated invoice-lines when removing an invoice
* [MODINVOSTO-18](https://issues.folio.org/browse/MODINVOSTO-18) - Add foreign key constraints on voucherLine.voucherId
* [MODINVOSTO-16](https://issues.folio.org/browse/MODINVOSTO-16) - Align invoice and order sample data
* [MODINVOSTO-14](https://issues.folio.org/browse/MODINVOSTO-14) - Implement GET /voucher-storage/voucher-number/start
* [MODINVOSTO-13](https://issues.folio.org/browse/MODINVOSTO-13) - Implement POST /voucher-storage/voucher-number/start/<value>
* [MODINVOSTO-12](https://issues.folio.org/browse/MODINVOSTO-12) - Implement GET /voucher-storage/voucher-number
* [MODINVOSTO-11](https://issues.folio.org/browse/MODINVOSTO-11) - Implement basic CRUD for voucher-lines
* [MODINVOSTO-9](https://issues.folio.org/browse/MODINVOSTO-9) - Voucher/voucherLine schemas and example data
* [MODINVOSTO-8](https://issues.folio.org/browse/MODINVOSTO-8) - Implement basic CRUD for vouchers
* [MODINVOSTO-7](https://issues.folio.org/browse/MODINVOSTO-7) - Implement the invoice-line-number API
* [MODINVOSTO-6](https://issues.folio.org/browse/MODINVOSTO-6) - Implement invoice-number API
* [MODINVOSTO-4](https://issues.folio.org/browse/MODINVOSTO-4) - Implement Basic CRUD APIs for invoice-lines
* [MODINVOSTO-3](https://issues.folio.org/browse/MODINVOSTO-3) - Implement Basic CRUD APIs for invoices
* [MODINVOSTO-2](https://issues.folio.org/browse/MODINVOSTO-2) - Define APIs
* [MODINVOSTO-1](https://issues.folio.org/browse/MODINVOSTO-1) - Project Setup

### Bug Fixes
* [MODINVOSTO-25](https://issues.folio.org/browse/MODINVOSTO-25) - Improve order-invoice sample data
