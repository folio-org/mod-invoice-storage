UPDATE ${myuniversity}_${mymodule}.vouchers
SET
  jsonb = jsonb || jsonb_build_object('batchGroupId', '2a2cb998-1437-41d1-88ad-01930aaeadd5')
WHERE
  NOT jsonb ? 'batchGroupId';

UPDATE ${myuniversity}_${mymodule}.vouchers
SET
  jsonb = jsonb - 'batchNumber'
WHERE
  jsonb ? 'batchNumber';