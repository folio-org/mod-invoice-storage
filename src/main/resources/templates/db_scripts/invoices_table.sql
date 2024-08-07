CREATE INDEX IF NOT EXISTS invoices_invoice_date_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'invoiceDate')),600), lower(f_unaccent(invoices.jsonb->>'invoiceDate')));

CREATE INDEX IF NOT EXISTS invoice_num_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'vendorInvoiceNo')),600), lower(f_unaccent(invoices.jsonb->>'vendorInvoiceNo')));

CREATE INDEX IF NOT EXISTS invoices_status_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'status')),600), lower(f_unaccent(invoices.jsonb->>'status')));

CREATE INDEX IF NOT EXISTS invoices_invoice_total_sort ON ${myuniversity}_${mymodule}.invoices
  (left(lower(f_unaccent(invoices.jsonb->>'invoiceTotal')),600), lower(f_unaccent(invoices.jsonb->>'invoiceTotal')));
