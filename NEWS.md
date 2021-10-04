## 5.3.0 - Unreleased

## 5.2.0 - Released
The primary focus of this release was to perform migration scripts according to schema changes

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v5.1.0...v5.2.0)

### Stories
* [MODINVOSTO-122](https://issues.folio.org/browse/MODINVOSTO-122) - Migration script for Fund code in the fund distribution
* [MODINVOSTO-119](https://issues.folio.org/browse/MODINVOSTO-119) - Cancel invoice: Update voucher and transaction schema with new fields
* [MODINVOSTO-73](https://issues.folio.org/browse/MODINVOSTO-73) - poNumbers cross-module migration


## 5.1.0 - Released
The primary focus of this release was to update RMB version up to v33.0.0, various schema fields updated as well

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v5.0.0...v5.1.0)

### Stories
* [MODINVOSTO-114](https://issues.folio.org/browse/MODINVOSTO-114) - Add vendor data to voucher schema
* [MODINVOSTO-113](https://issues.folio.org/browse/MODINVOSTO-113) - Add field "VendorAddress" into batch voucher schemas
* [MODINVOSTO-109](https://issues.folio.org/browse/MODINVOSTO-109) - mod-invoice-storage: Update RMB
* [MODINVOSTO-103](https://issues.folio.org/browse/MODINVOSTO-103) - Add personal data disclosure form

### Bug Fixes

* [MODINVOSTO-115](https://issues.folio.org/browse/MODINVOSTO-115) - Example API JSON, "invoice_line_collection.sample", has invalid field


## 5.0.0 - Released

The primary focus of this release was to add some additional fields in voucher schema, upgrade invoice totals, upgrade RMB and adding
support pair of "refNumber" and "refNumberType" in Invoice line should be an array

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v4.1.2...v5.0.0)

### Stories
* [MODINVOSTO-106](https://issues.folio.org/browse/MODINVOSTO-106) - Add fields "enclosureNeeded" and "accountNo" into voucher schema
* [MODINVOSTO-105](https://issues.folio.org/browse/MODINVOSTO-105) - Delete document while deleting invoice
* [MODINVOSTO-100](https://issues.folio.org/browse/MODINVOSTO-100) - mod-invoice-storage: Update RMB
* [MODINVOSTO-97](https://issues.folio.org/browse/MODINVOSTO-97) - Write migration script for lock total value
* [MODINVOSTO-96](https://issues.folio.org/browse/MODINVOSTO-96) - Update invoice totals fields
* [MODINVOSTO-92](https://issues.folio.org/browse/MODINVOSTO-92) - Add field "accountNo" for supporting vendors that have more than one account
* [MODINVOSTO-89](https://issues.folio.org/browse/MODINVOSTO-89) - Capture date paid in invoice schema
* [MODINVOSTO-81](https://issues.folio.org/browse/MODINVOSTO-81) - Support pair of "refNumber" and "refNumberType" in Invoice line should be an array
* [MODINVOSTO-75](https://issues.folio.org/browse/MODINVOSTO-75) - Migration script for removing invoice number from the invoice line number

## 4.1.2 - Released

The primary focus of this release was to fix Adding certain prefix and suffixes prevents user from being able to save

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v4.1.1...v4.1.2)

### Bug Fixes
* [MODORDSTOR-197](https://issues.folio.org/browse/MODORDSTOR-197) - Adding certain prefix and suffixes prevents user from being able to save

## 4.1.1 - Released

The primary focus of this release was to fix RMB and logging issues

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v4.1.0...v4.1.1)

### Bug Fixes
* [MODINVOSTO-93](https://issues.folio.org/browse/MODINVOSTO-93) - Update RMB up to v31.1.5
* [MODINVOSTO-90](https://issues.folio.org/browse/MODINVOSTO-90) - No logging in honeysuckle version


## 4.1.0 - Released

The primary focus of this release was to migrate to JDK 11 with new RMB, update fund distribution schema

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v4.0.1...v4.1.0)

### Stories
* [MODINVOSTO-80](https://issues.folio.org/browse/MODINVOSTO-80) - mod-invoice-storage: Update RMB
* [MODINVOSTO-76](https://issues.folio.org/browse/MODINVOSTO-76) - Migrate mod-invoice-storage to JDK 11
* [MODINVOSTO-71](https://issues.folio.org/browse/MODINVOSTO-71) - Update fundDistribution schema

## 4.0.1 - Released

This is a bugfix release and contains fixes for full text index creation.
Also module was migrated on RMB 30.1.0

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v4.0.0...v4.0.1)

### Bug Fixes
* [MODINVOSTO-72](https://issues.folio.org/browse/MODINVOSTO-72) - Add invoice_line.description fullTextIndex
* [MODINVOSTO-69](https://issues.folio.org/browse/MODINVOSTO-69) - Migration script populate "fundDistribution.code" into old voucher lines

## 4.0.0 - Released

The focus of this release was implementing cascade deletion and create view for search and filtering

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v3.1.1...v4.0.0)

### Stories
* [MODINVOSTO-67](https://issues.folio.org/browse/MODINVOSTO-67) - Final verification migration scripts before release Q2 2020
* [MODINVOSTO-66](https://issues.folio.org/browse/MODINVOSTO-66) - mod-invoice-storage: Update to RMB v30.0.1
* [MODINVOSTO-58](https://issues.folio.org/browse/MODINVOSTO-58) - Invoicing schema changes
* [MODINVOSTO-57](https://issues.folio.org/browse/MODINVOSTO-57) - Implementing cascade deletion for batch-voucher and batch-voucher-exports APIs

### Bug Fixes
* [MODINVOSTO-68](https://issues.folio.org/browse/MODINVOSTO-68) - Migration scripts for breaking-changes in invoice app schemas are missed

## 3.1.1 - Released

Fix data migration script for batch groups

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v3.1.0...v3.1.1)

### Bug Fixes
* [MODINVOSTO-61](https://issues.folio.org/browse/MODINVOSTO-61) - Data migration script fails due to FK constraint

## 3.1.0 - Released

The focus of this release was to introduce Batch Voucher Exports CRUD APIs

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v3.0.0...v3.1.0)

### Stories
* [MODINVOSTO-54](https://issues.folio.org/browse/MODINVOSTO-54) - Create batch-voucher-exports CRUD API
* [MODINVOSTO-53](https://issues.folio.org/browse/MODINVOSTO-53) - Create batch-voucher API
* [MODINVOSTO-52](https://issues.folio.org/browse/MODINVOSTO-52) - Create batch-voucher-export configuration credentials API
* [MODINVOSTO-51](https://issues.folio.org/browse/MODINVOSTO-51) - Create batch-voucher-configurations CRUD API
* [MODINVOSTO-50](https://issues.folio.org/browse/MODINVOSTO-50) - Create batch-group CRUD API
* [MODINVOSTO-48](https://issues.folio.org/browse/MODINVOSTO-48) - Batch voucher export schema changes

## 3.0.0 - Released

The focus of this release was to improve invoices and vouchers processing

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v2.0.0...v3.0.0)

### Stories
* [MODINVOSTO-45](https://issues.folio.org/browse/MODINVOSTO-45) - Update RMB to 29.0.1
* [MODINVOSTO-44](https://issues.folio.org/browse/MODINVOSTO-44) - Use JVM features to manage container memory
* [MODINVOSTO-41](https://issues.folio.org/browse/MODINVOSTO-41) - Allow fund distributions to be specified as amount or percentage (breaking changes)
* [MODINVOSTO-40](https://issues.folio.org/browse/MODINVOSTO-40) - Remove unique index on voucherNumber

## 2.0.0 - Released

The primary focus of this release was to allow cross table index queries and provide the APIs for managing invoice attachments.

[Full Changelog](https://github.com/folio-org/mod-invoice-storage/compare/v1.0.0...v2.0.0)

### Stories
* [MODINVOICE-77](https://issues.folio.org/browse/MODINVOICE-77) - Invoice and invoiceLine schema updates (breaking changes)
* [MODINVOSTO-35](https://issues.folio.org/browse/MODINVOSTO-35) - Cleanup acquisition-units-assignments APIs/views/etc. (breaking changes)
* [MODINVOSTO-34](https://issues.folio.org/browse/MODINVOSTO-34) - Implement API for invoice attachments (links/documents)
* [MODINVOSTO-31](https://issues.folio.org/browse/MODINVOSTO-31) - Voucher/voucher line cross table index queries
* [MODINVOSTO-28](https://issues.folio.org/browse/MODINVOSTO-28) - Invoice/invoice line cross table index queries

### Bug Fixes
* [MODINVOSTO-38](https://issues.folio.org/browse/MODINVOSTO-38) - MOD-INVOICE-STORAGE fails after several runs of API tests

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
