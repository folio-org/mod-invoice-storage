UPDATE ${myuniversity}_${mymodule}.voucher_lines
SET jsonb =
    (
      -- Update each fundDistributions element renaming 'percentage' to 'value' and adding 'distributionType' with 'percentage' value
      SELECT jsonb_set(jsonb, '{fundDistributions}', jsonb_agg(distrib - 'percentage' || jsonb_build_object('value', coalesce(distrib -> 'percentage', distrib -> 'value'), 'distributionType', coalesce(distrib ->> 'distributionType', 'percentage'))))
      FROM jsonb_array_elements(jsonb -> 'fundDistributions') distrib
    )
-- Limit to only those records which have any 'fundDistributions' and are not yet updated to make script re-runnable
WHERE jsonb_array_length(jsonb -> 'fundDistributions') > 0
  AND (SELECT count(*)
       FROM jsonb_array_elements(jsonb -> 'fundDistributions') elem
       WHERE elem -> 'distributionType' IS NULL OR elem -> 'percentage' IS NOT NULL) > 0;
