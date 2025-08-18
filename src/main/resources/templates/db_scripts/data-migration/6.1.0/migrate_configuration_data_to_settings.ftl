<#if mode.name() == "UPDATE">

INSERT INTO ${myuniversity}_${mymodule}.settings (id, jsonb)
SELECT C.id, jsonb_build_object(
  'id', (C.jsonb->>'id'),
  'key', (C.jsonb->>'configName'),
  'value', (C.jsonb->>'value'),
  'metadata', (C.jsonb->'metadata')
) VALUE
FROM ${myuniversity}_mod_configuration.config_data C
WHERE (C.jsonb->>'module') = 'INVOICE' AND NOT EXISTS(
  SELECT 1
  FROM ${myuniversity}_${mymodule}.settings S
  WHERE (S.jsonb->>'key') = (C.jsonb->>'configName')
);

</#if>
