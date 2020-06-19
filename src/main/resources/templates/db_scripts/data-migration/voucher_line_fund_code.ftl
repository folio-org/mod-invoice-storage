<#if mode.name() == "UPDATE">
WITH fund_distr AS (
  SELECT array_to_json(array_agg(fd|| jsonb_build_object('code', fund.jsonb->>'code'))) AS fund_distros
    FROM ${myuniversity}_${mymodule}.voucher_lines AS voucher_lines,
          jsonb_array_elements(voucher_lines.jsonb->'fundDistributions') AS fd
    INNER JOIN ${myuniversity}_mod_finance_storage.fund AS fund ON fund.id=(fd->>'fundId')::uuid
    GROUP BY voucher_lines.jsonb
)
UPDATE ${myuniversity}_${mymodule}.voucher_lines
  SET jsonb=jsonb_set(jsonb, '{fundDistributions}', fund_distr.fund_distros::jsonb)
  FROM fund_distr;
</#if>
