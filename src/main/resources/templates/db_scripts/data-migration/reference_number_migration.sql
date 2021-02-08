UPDATE ${myuniversity}_${mymodule}.invoice_lines
SET
  jsonb = jsonb - 'vendorRefNo' || jsonb_build_object('referenceNumbers', json_build_array(jsonb_build_object(
        'refNumber', jsonb->>'vendorRefNo',
        'vendorDetailsSource', 'InvoiceLine')))
WHERE
  jsonb ? 'vendorRefNo';