-- Copy enclosureNeeded and accountNo from the invoice
UPDATE ${myuniversity}_${mymodule}.vouchers v
SET jsonb = jsonb ||
  jsonb_strip_nulls(jsonb_build_object('enclosureNeeded', i.jsonb->'enclosureNeeded', 'accountNo', i.jsonb->'accountNo'))
FROM ${myuniversity}_${mymodule}.invoices i
WHERE v.invoiceID = i.id;
