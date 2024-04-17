UPDATE ${myuniversity}_${mymodule}.invoices AS i
SET jsonb = jsonb || jsonb_build_object('voucherNumber', v.jsonb ->> 'voucherNumber')
  FROM ${myuniversity}_${mymodule}.vouchers AS v
WHERE i.id = v.invoiceId;
