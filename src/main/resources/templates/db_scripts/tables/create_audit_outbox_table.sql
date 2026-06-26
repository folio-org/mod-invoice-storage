CREATE TABLE IF NOT EXISTS outbox_event_log (
  event_id uuid NOT NULL PRIMARY KEY,
  entity_type text NOT NULL,
  action text NOT NULL,
  payload jsonb,
  original_payload jsonb
);

ALTER TABLE outbox_event_log ADD COLUMN IF NOT EXISTS original_payload jsonb;
