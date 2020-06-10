-- set default false value for invoice.enclosureNeeded
UPDATE ${myuniversity}_${mymodule}.invoices
	SET jsonb=jsonb || jsonb_build_object('enclosureNeeded', false)
	WHERE NOT jsonb ? 'enclosureNeeded';

-- set default false value for invoice.adjustments[].exportToAccounting
WITH adjustment AS (
	SELECT array_to_json(array_agg(CASE WHEN NOT adj ? 'exportToAccounting'
												THEN adj|| jsonb_build_object('exportToAccounting', false)
												ELSE adj
												END))AS adjustments
		FROM ${myuniversity}_${mymodule}.invoices AS invoice,
			jsonb_array_elements(invoice.jsonb->'adjustments') AS adj
		GROUP BY jsonb
)
UPDATE ${myuniversity}_${mymodule}.invoices
	SET jsonb=jsonb_set(jsonb, '{adjustments}', adjustment.adjustments::jsonb)
	FROM adjustment;

-- set default false value for invoice_line.adjustments[].exportToAccounting
WITH adjustment AS (
	SELECT array_to_json(array_agg(CASE WHEN NOT adj ? 'exportToAccounting'
												THEN adj|| jsonb_build_object('exportToAccounting', false)
												ELSE adj
												END))AS adjustments
		FROM ${myuniversity}_${mymodule}.invoice_lines AS invoice_line,
			jsonb_array_elements(invoice_line.jsonb->'adjustments') AS adj
		GROUP BY jsonb
)
UPDATE ${myuniversity}_${mymodule}.invoice_lines
	SET jsonb=jsonb_set(jsonb, '{adjustments}', adjustment.adjustments::jsonb)
	FROM adjustment;
