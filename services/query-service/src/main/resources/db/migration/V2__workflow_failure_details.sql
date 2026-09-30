ALTER TABLE operation_read_model
  ADD COLUMN IF NOT EXISTS failure_stage VARCHAR(30);

ALTER TABLE user_request_read_model
  ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500),
  ADD COLUMN IF NOT EXISTS failure_stage VARCHAR(30);
