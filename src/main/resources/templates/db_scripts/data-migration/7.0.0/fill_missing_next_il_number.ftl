<#if mode.name() == "UPDATE">
  WITH missing_next_numbers AS (
    SELECT invoice.id, COALESCE(MAX((il.jsonb ->> 'invoiceLineNumber')::int), 0) + 1 AS number
      FROM ${myuniversity}_${mymodule}.invoices invoice
      LEFT JOIN ${myuniversity}_${mymodule}.invoice_lines il ON il.invoiceId = invoice.id
      WHERE invoice.jsonb -> 'nextInvoiceLineNumber' IS NULL
      GROUP BY invoice.id
  )

  UPDATE ${myuniversity}_${mymodule}.invoices invoice
  SET jsonb = jsonb_set(invoice.jsonb, '{nextInvoiceLineNumber}', to_jsonb(mnn.number))
  FROM missing_next_numbers mnn
  WHERE invoice.id = mnn.id;
</#if>
