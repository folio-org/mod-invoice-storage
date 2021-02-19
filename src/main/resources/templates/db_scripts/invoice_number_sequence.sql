CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.invoice_number NO MAXVALUE START WITH 10000 CACHE 1 NO CYCLE;
GRANT USAGE ON SEQUENCE ${myuniversity}_${mymodule}.invoice_number TO ${myuniversity}_${mymodule};

DROP INDEX IF EXISTS invoices_invoice_number_unique_idx;
