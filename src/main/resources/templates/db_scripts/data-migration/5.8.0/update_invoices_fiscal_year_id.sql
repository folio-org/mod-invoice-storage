WITH updated_invoices AS (
  SELECT
    inv.id AS invoice_id,
    (
      SELECT to_jsonb(trx.jsonb->>'fiscalYearId')
      FROM ${myuniversity}_mod_finance_storage.transaction AS trx
      WHERE trx.jsonb->>'sourceInvoiceId' = inv.id::text
      LIMIT 1
    ) AS fiscal_year_id
  FROM ${myuniversity}_${mymodule}.invoices AS inv
  WHERE inv.jsonb->>'fiscalYearId' IS NULL
)

UPDATE ${myuniversity}_${mymodule}.invoices AS inv
SET jsonb = jsonb_set(inv.jsonb, '{fiscalYearId}', ui.fiscal_year_id)
FROM updated_invoices AS ui
WHERE inv.id = ui.invoice_id
AND ui.fiscal_year_id IS NOT NULL;
