-- V3.2: no existe reevaluación. Cada solicitud mantiene un único workflow/correlationId.
CREATE UNIQUE INDEX IF NOT EXISTS uk_credit_request_correlation ON credit_request(correlation_id);
