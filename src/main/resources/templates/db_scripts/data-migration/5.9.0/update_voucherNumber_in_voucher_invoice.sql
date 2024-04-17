UPDATE ${myuniversity}_${mymodule}.invoices AS i
SET jsonb = i.jsonb || jsonb_build_object('voucherNumber', v.jsonb ->> 'voucherNumber')
  FROM ${myuniversity}_${mymodule}.vouchers AS v
WHERE i.id = v.invoiceId;
