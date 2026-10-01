package com.finanscore.support.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanscore.contracts.*;
import com.finanscore.support.observability.ObservabilityContext;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;


//Guarda el evento en BD antes de publicarlo. 
//“Evito guardar negocio en PostgreSQL y perder el evento si Kafka falla.”

@Service
public class OutboxService {
	private final OutboxEventRepository repo;
	private final ObjectMapper om;

	public OutboxService(OutboxEventRepository r, ObjectMapper o) {
		repo = r;
		om = o;
	}

	public EventEnvelope append(String type, String aggregateId, String correlationId, String causationId,
			String traceId, String source, Map<String, Object> payload) {
		try {
			String eventId = UUID.randomUUID().toString();
			String effectiveTraceId = (traceId == null || traceId.isBlank()) ? ObservabilityContext.currentTraceId()
					: traceId;
			EventEnvelope e = new EventEnvelope(eventId, type, 1, Instant.now(), correlationId, causationId,
					effectiveTraceId, source, aggregateId, payload);
			OutboxEventEntity x = new OutboxEventEntity();
			x.id = UUID.randomUUID().toString();
			x.eventId = eventId;
			x.eventType = type;
			x.aggregateId = aggregateId;
			x.topic = EventTopics.fromType(type);
			x.eventKey = aggregateId;
			x.payload = om.writeValueAsString(e);
			x.status = "PENDING";
			x.createdAt = Instant.now();
			repo.save(x);
			return e;
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo serializar outbox", ex);
		}
	}
}
