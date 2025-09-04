<#if mode.name() == "UPDATE">

INSERT INTO ${myuniversity}_${mymodule}.settings (id, jsonb)
SELECT C.id, jsonb_build_object(
  'id', (C.jsonb->>'id'),
  'key', (C.jsonb->>'configName'),
  'value', (C.jsonb->>'value'),
  'metadata', (C.jsonb->'metadata')
) VALUE
FROM ${myuniversity}_mod_configuration.config_data C
WHERE (C.jsonb->>'module') = 'INVOICE'
  AND (C.jsonb->>'configName') != 'INVOICE.adjustments'
ON CONFLICT (lower(${myuniversity}_${mymodule}.f_unaccent(jsonb->>'key'::text))) DO NOTHING;

INSERT INTO ${myuniversity}_${mymodule}.adjustment_presets (id, jsonb)
SELECT C.id, jsonb_build_object('id', C.id, 'metadata', (C.jsonb->'metadata')) || (C.jsonb->>'value')::jsonb VALUE
FROM ${myuniversity}_mod_configuration.config_data C
WHERE (C.jsonb->>'module') = 'INVOICE'
  AND (C.jsonb->>'configName') = 'INVOICE.adjustments';

</#if>
