UPDATE ${myuniversity}_${mymodule}.vouchers as vouchers
SET jsonb = vouchers.jsonb ||
    (
      SELECT jsonb_build_object('vendorId', invoices.jsonb->>'vendorId')
      FROM ${myuniversity}_${mymodule}.invoices as invoices
      WHERE vouchers.invoiceId = invoices.id
    )
WHERE NOT vouchers.jsonb ? 'vendorId';