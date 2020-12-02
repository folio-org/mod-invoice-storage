UPDATE ${myuniversity}_${mymodule}.invoices AS invoices
SET jsonb = jsonb_strip_nulls(invoices.jsonb - 'total' - 'subTotal' ||
        CASE
            WHEN invoices.jsonb ? 'total' AND (invoices.jsonb->'lockTotal')::text='true' THEN  jsonb_build_object('lockTotal', (invoices.jsonb->>'total')::decimal)
			WHEN (invoices.jsonb->'lockTotal')::text='false' THEN jsonb_build_object('lockTotal', null)
			ELSE '{}'
        END);
