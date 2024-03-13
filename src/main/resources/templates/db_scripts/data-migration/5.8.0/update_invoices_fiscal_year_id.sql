WITH updated_invoices AS (
    SELECT inv.id AS invoice_id,
           to_jsonb(trx.jsonb->>'fiscalYearId') AS fiscal_year_id
    FROM ${myuniversity}_${mymodule}.invoices AS inv
    JOIN ${myuniversity}_mod_finance_storage.transaction AS trx
        ON trx.jsonb->>'sourceInvoiceId' = inv.id::text
    WHERE inv.jsonb->>'fiscalYearId' IS NULL
)
UPDATE ${myuniversity}_${mymodule}.invoices AS inv
SET jsonb = jsonb_set(inv.jsonb, '{fiscalYearId}', ui.fiscal_year_id)
FROM updated_invoices AS ui
WHERE inv.id = ui.invoice_id
  AND ui.fiscal_year_id IS NOT NULL;

GRANT ${myuniversity}_finance_storage TO ${myuniversity}_mod_invoice_storage;
