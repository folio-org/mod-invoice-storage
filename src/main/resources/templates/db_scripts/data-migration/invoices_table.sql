CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.invoice_number NO MAXVALUE START WITH 10000 CACHE 1 NO CYCLE;
GRANT USAGE ON SEQUENCE ${myuniversity}_${mymodule}.invoice_number TO ${myuniversity}_${mymodule};
CREATE UNIQUE INDEX IF NOT EXISTS invoices_invoice_number_unique_idx ON ${myuniversity}_${mymodule}.invoices ((jsonb->>'invoiceNumber'));

UPDATE ${myuniversity}_${mymodule}.invoices
SET
  jsonb = jsonb || jsonb_build_object('batchGroupId', '2a2cb998-1437-41d1-88ad-01930aaeadd5')
WHERE
  NOT jsonb ? 'batchGroupId';
