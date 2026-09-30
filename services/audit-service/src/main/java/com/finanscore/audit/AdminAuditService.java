package com.finanscore.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminAuditService {
    private final AuditEventRepository events;
    private final DltEventRepository dlts;
    private final ObjectMapper om;

    public AdminAuditService(AuditEventRepository events, DltEventRepository dlts, ObjectMapper om) {
        this.events = events;
        this.dlts = dlts;
        this.om = om;
    }

    public List<AuditEventView> audit(String requestId, String correlationId, String eventType, String source, int limit) {
        return events.findTop2000ByOrderByOccurredAtDesc().stream()
            .filter(e -> blank(requestId) || requestId.equals(e.aggregateId))
            .filter(e -> blank(correlationId) || correlationId.equals(e.correlationId))
            .filter(e -> blank(eventType) || eventType.equalsIgnoreCase(e.eventType))
            .filter(e -> blank(source) || source.equalsIgnoreCase(e.source))
            .sorted(Comparator.comparing((AuditEventEntity e) -> e.occurredAt).reversed())
            .limit(Math.max(1, Math.min(limit, 500)))
            .map(this::view)
            .toList();
    }

    public List<DltView> dlt(String requestId, String correlationId, String sourceTopic, int limit) {
        return dlts.findTop500ByOrderByRecordedAtDesc().stream()
            .filter(e -> blank(requestId) || requestId.equals(e.aggregateId))
            .filter(e -> blank(correlationId) || correlationId.equals(e.correlationId))
            .filter(e -> blank(sourceTopic) || e.sourceTopic.contains(sourceTopic))
            .limit(Math.max(1, Math.min(limit, 500)))
            .map(e -> new DltView(e.id, e.sourceTopic, e.sourceService, e.eventId, e.eventType, e.aggregateId, e.correlationId, e.traceId, e.exceptionMessage, e.retryCount, e.recordedAt, e.payloadJson))
            .toList();
    }

    private AuditEventView view(AuditEventEntity e) {
        return new AuditEventView(e.eventId, e.eventType, e.aggregateId, e.correlationId, e.causationId, e.traceId, e.source, e.occurredAt, payload(e));
    }

    private Map<String,Object> payload(AuditEventEntity e) {
        try {
            return om.readValue(e.payloadJson, new TypeReference<Map<String,Object>>() {});
        } catch (Exception ex) {
            return Map.of("raw", e.payloadJson);
        }
    }

    private static boolean blank(String x) { return x == null || x.isBlank(); }

    public record AuditEventView(String eventId,String eventType,String requestId,String correlationId,String causationId,String traceId,String source,Instant occurredAt,Map<String,Object> payload){}
    public record DltView(UUID id,String sourceTopic,String sourceService,String eventId,String eventType,String requestId,String correlationId,String traceId,String error,Integer retryCount,Instant recordedAt,String payload){}
}
