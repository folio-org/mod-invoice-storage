UPDATE ${myuniversity}_${mymodule}.invoice_lines
SET jsonb = jsonb || jsonb_build_object('invoiceLineNumber', coalesce(nullif(split_part(jsonb ->> 'invoiceLineNumber', '-', 2), ''),
                                                                      jsonb ->> 'invoiceLineNumber'));
