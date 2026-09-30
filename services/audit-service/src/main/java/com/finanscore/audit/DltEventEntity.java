package com.finanscore.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="dlt_event", indexes={
    @Index(name="idx_dlt_aggregate", columnList="aggregate_id"),
    @Index(name="idx_dlt_correlation", columnList="correlation_id"),
    @Index(name="idx_dlt_recorded", columnList="recorded_at")
})
public class DltEventEntity {
    @Id public UUID id;
    @Column(name="source_topic", nullable=false, length=160) public String sourceTopic;
    @Column(name="partition_no") public Integer partition;
    @Column(name="offset_no") public Long offset;
    @Column(name="event_id", length=64) public String eventId;
    @Column(name="event_type", length=100) public String eventType;
    @Column(name="aggregate_id", length=100) public String aggregateId;
    @Column(name="correlation_id", length=64) public String correlationId;
    @Column(name="trace_id", length=100) public String traceId;
    @Column(name="source_service", length=100) public String sourceService;
    @Column(name="exception_message", length=2000) public String exceptionMessage;
    @Column(name="retry_count") public Integer retryCount;

    @Column(name="payload_json", columnDefinition="TEXT") public String payloadJson;
    @Column(name="headers_json", columnDefinition="TEXT") public String headersJson;

    @Column(name="recorded_at", nullable=false) public Instant recordedAt;
}
