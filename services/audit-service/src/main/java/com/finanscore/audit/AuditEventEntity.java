package com.finanscore.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event", indexes = {
    @Index(name = "idx_audit_correlation", columnList = "correlation_id"),
    @Index(name = "idx_audit_aggregate", columnList = "aggregate_id")
})
public class AuditEventEntity {
    @Id public UUID id;
    @Column(name = "event_id", nullable = false, unique = true) public String eventId;
    @Column(name = "event_type", nullable = false) public String eventType;
    @Column(name = "correlation_id") public String correlationId;
    @Column(name = "causation_id") public String causationId;
    @Column(name = "trace_id") public String traceId;
    @Column(name = "aggregate_id") public String aggregateId;
    @Column(nullable = false) public String source;

    // PostgreSQL TEXT debe mapearse como String normal. @Lob hace que Hibernate/JDBC
    // intente usar la API de Large Object (OID), que falla en lecturas autocommit.
    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    public String payloadJson;

    @Column(name = "occurred_at", nullable = false) public Instant occurredAt;
    @Column(name = "recorded_at", nullable = false) public Instant recordedAt;
}
