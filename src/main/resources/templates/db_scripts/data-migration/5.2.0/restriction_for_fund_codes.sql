-- Replace colon ":" with hyphen "-" for fund codes in fund distributions
UPDATE ${myuniversity}_${mymodule}.invoice_lines
SET jsonb =
      (
        -- Update each fundDistributions element
        SELECT jsonb_set(jsonb, '{fundDistributions}', jsonb_agg(
            distrib || jsonb_build_object('code', replace(distrib ->> 'code', ':', '-'))
          ))
        FROM jsonb_array_elements(jsonb -> 'fundDistributions') distrib
      )
-- Limit to only those records which have any 'fundDistributions' and are not yet updated to make script re-runnable
WHERE jsonb_array_length(jsonb -> 'fundDistributions') > 0
  AND (SELECT count(*)
       FROM jsonb_array_elements(jsonb -> 'fundDistributions') elem
       WHERE elem ->> 'code' LIKE '%:%') > 0;
