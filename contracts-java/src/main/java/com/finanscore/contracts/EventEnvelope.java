package com.finanscore.contracts;
import java.time.Instant; import java.util.Map;
public record EventEnvelope(String eventId,String eventType,int eventVersion,Instant occurredAt,String correlationId,String causationId,String traceId,String source,String aggregateId,Map<String,Object> payload) {}
