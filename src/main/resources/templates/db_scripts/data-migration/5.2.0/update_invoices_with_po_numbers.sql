CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE OR REPLACE FUNCTION ${myuniversity}_${mymodule}.update_invoices_with_po_numbers(_invoice_vs_po_numbers_list jsonb) RETURNS VOID as $$
BEGIN
    WITH _invoices_with_updated_po_numbers AS (
      SELECT jsonb_set(jsonb, '{poNumbers}',
          coalesce((SELECT _invoices_vs_po_numbers -> 'poNumbers' FROM jsonb_array_elements(_invoice_vs_po_numbers_list) _invoices_vs_po_numbers
            WHERE _invoices_vs_po_numbers ->> 'invoiceId' = jsonb ->> 'id'), jsonb -> 'poNumbers', '[]')
            ) as _updated_invoices
      FROM ${myuniversity}_${mymodule}.invoices
    )
    UPDATE ${myuniversity}_${mymodule}.invoices _origin_invoices
    SET jsonb = _updated_invoices
    FROM _invoices_with_updated_po_numbers _updated_invoices
    WHERE _origin_invoices.jsonb->>'id' = _updated_invoices ->>'id';
END;
$$ LANGUAGE plpgsql;


