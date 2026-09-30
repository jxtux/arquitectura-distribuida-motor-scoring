CREATE TABLE IF NOT EXISTS outbox_event(
  outbox_event_id VARCHAR(36) PRIMARY KEY,
  event_id VARCHAR(36) NOT NULL UNIQUE,
  event_type VARCHAR(80) NOT NULL,
  aggregate_id VARCHAR(100) NOT NULL,
  topic VARCHAR(120) NOT NULL,
  event_key VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_event(status,created_at);

CREATE TABLE IF NOT EXISTS processed_event(
  processed_event_id UUID PRIMARY KEY,
  event_id VARCHAR(36) NOT NULL,
  consumer_name VARCHAR(100) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_processed_event_consumer UNIQUE(event_id,consumer_name)
);
