CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.voucher_number MINVALUE 0 NO MAXVALUE CACHE 1 NO CYCLE;
ALTER SEQUENCE ${myuniversity}_${mymodule}.voucher_number OWNER TO ${myuniversity}_${mymodule};
