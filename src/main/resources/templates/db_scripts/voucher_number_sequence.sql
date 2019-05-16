CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.voucher_number MINVALUE 0 NO MAXVALUE CACHE 1 NO CYCLE;
GRANT USAGE ON SEQUENCE ${myuniversity}_${mymodule}.voucher_number TO ${myuniversity}_${mymodule};
