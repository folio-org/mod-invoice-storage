CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.voucher_number NO MAXVALUE START WITH 10000 CACHE 1 NO CYCLE;
GRANT USAGE ON SEQUENCE ${myuniversity}_${mymodule}.voucher_number TO ${myuniversity}_${mymodule};
CREATE UNIQUE INDEX IF NOT EXISTS invoices_voucher_number_unique_idx ON ${myuniversity}_${mymodule}.invoices ((jsonb->>'voucherNumber'));
