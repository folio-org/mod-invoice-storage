CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.voucher_number MINVALUE 0 NO MAXVALUE CACHE 1 NO CYCLE;
ALTER SEQUENCE ${myuniversity}_${mymodule}.voucher_number OWNER TO ${myuniversity}_${mymodule};

UPDATE ${myuniversity}_${mymodule}.vouchers
SET
  jsonb = jsonb || jsonb_build_object('batchGroupId', '2a2cb998-1437-41d1-88ad-01930aaeadd5')
WHERE
  NOT jsonb ? 'batchGroupId';
