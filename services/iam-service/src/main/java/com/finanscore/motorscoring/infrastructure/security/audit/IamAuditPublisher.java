package com.finanscore.motorscoring.infrastructure.security.audit;

import com.finanscore.support.outbox.OutboxService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class IamAuditPublisher {
    private final OutboxService outbox;

    public IamAuditPublisher(OutboxService outbox) {
        this.outbox = outbox;
    }

    public void emit(String action, String subject, String correlationId, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("subject", subject == null ? "unknown" : subject);
        if (extra != null) payload.putAll(extra);
        String aggregate = subject == null || subject.isBlank() ? "iam" : subject;
        String correlation = correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
        outbox.append("IamAuditEvent", aggregate, correlation, null, null, "iam-service", payload);
    }
}
