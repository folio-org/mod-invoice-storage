CREATE INDEX IF NOT EXISTS invoices_invoice_date_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'invoiceDate')),600), lower(f_unaccent(invoices.jsonb->>'invoiceDate')));

CREATE INDEX IF NOT EXISTS invoices_vendor_invoice_no_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'vendorInvoiceNo')),600), lower(f_unaccent(invoices.jsonb->>'vendorInvoiceNo')));

CREATE INDEX IF NOT EXISTS invoices_status_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'status')),600), lower(f_unaccent(invoices.jsonb->>'status')));

CREATE INDEX IF NOT EXISTS invoices_invoice_total_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'invoiceTotal')),600), lower(f_unaccent(invoices.jsonb->>'invoiceTotal')));

CREATE INDEX IF NOT EXISTS invoices_no_acq_unit ON ${myuniversity}_${mymodule}.invoices(jsonb)
  WHERE left(lower(f_unaccent(jsonb ->> 'acqUnitIds')), 600) NOT LIKE '[]';
