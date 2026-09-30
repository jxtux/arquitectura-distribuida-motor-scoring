CREATE TABLE operation_read_model(
  request_id UUID PRIMARY KEY,
  correlation_id VARCHAR(36) NOT NULL,
  user_id BIGINT,
  product_code VARCHAR(40),
  amount DECIMAL(18,2),
  currency VARCHAR(8),
  term_months INT,
  purpose VARCHAR(150),
  payment_status VARCHAR(30),
  workflow_status VARCHAR(40) NOT NULL,
  score_value INT,
  recommendation VARCHAR(40),
  model_version VARCHAR(20),
  report_id UUID,
  report_available BOOLEAN NOT NULL DEFAULT FALSE,
  notification_status VARCHAR(30),
  failure_reason VARCHAR(500),
  last_event_id VARCHAR(36),
  last_event_type VARCHAR(80),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uk_operation_correlation ON operation_read_model(correlation_id);
CREATE INDEX idx_operation_status_updated ON operation_read_model(workflow_status,updated_at DESC);
CREATE INDEX idx_operation_user_updated ON operation_read_model(user_id,updated_at DESC);
CREATE INDEX idx_operation_product_updated ON operation_read_model(product_code,updated_at DESC);

CREATE TABLE user_request_read_model(
  request_id UUID PRIMARY KEY,
  user_id BIGINT NOT NULL,
  correlation_id VARCHAR(36) NOT NULL,
  product_code VARCHAR(40),
  amount DECIMAL(18,2),
  currency VARCHAR(8),
  term_months INT,
  purpose VARCHAR(150),
  workflow_status VARCHAR(40) NOT NULL,
  score_value INT,
  recommendation VARCHAR(40),
  model_version VARCHAR(20),
  report_id UUID,
  report_available BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_user_request_user_created ON user_request_read_model(user_id,created_at DESC);
CREATE INDEX idx_user_request_status_updated ON user_request_read_model(workflow_status,updated_at DESC);

CREATE TABLE processed_projection_event(
  projection_key VARCHAR(180) PRIMARY KEY,
  event_id VARCHAR(36) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_projection_event_id ON processed_projection_event(event_id);
