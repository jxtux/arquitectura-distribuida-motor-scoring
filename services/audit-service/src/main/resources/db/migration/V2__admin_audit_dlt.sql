CREATE TABLE dlt_event(
  id UUID PRIMARY KEY,
  source_topic VARCHAR(160) NOT NULL,
  partition_no INT,
  offset_no BIGINT,
  event_id VARCHAR(64),
  event_type VARCHAR(100),
  aggregate_id VARCHAR(100),
  correlation_id VARCHAR(64),
  trace_id VARCHAR(100),
  source_service VARCHAR(100),
  exception_message VARCHAR(2000),
  retry_count INT,
  payload_json TEXT,
  headers_json TEXT,
  recorded_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_dlt_aggregate ON dlt_event(aggregate_id);
CREATE INDEX idx_dlt_correlation ON dlt_event(correlation_id);
CREATE INDEX idx_dlt_recorded ON dlt_event(recorded_at DESC);
