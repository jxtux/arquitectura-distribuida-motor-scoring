package com.finanscore.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanscore.contracts.EventEnvelope;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Component
public class AuditConsumer {
    private final ObjectMapper om; private final AuditEventRepository repo;
    public AuditConsumer(ObjectMapper o,AuditEventRepository r){om=o;repo=r;}

    @WithSpan("persist-audit-event")
    @KafkaListener(topicPattern="(payment\\.(validated|rejected)|credit\\.(request\\.created|evaluation\\.requested)|scoring\\.(calculated|failed)|report\\.(generated|failed)|notification\\.(sent|failed)|iam\\.audit)\\.v1",groupId="audit-service")
    @Transactional
    public void on(String raw)throws Exception{
        var e=om.readValue(raw,EventEnvelope.class);
        if(e.aggregateId()!=null)MDC.put("requestId",e.aggregateId());if(e.correlationId()!=null)MDC.put("correlationId",e.correlationId());if(e.eventId()!=null)MDC.put("eventId",e.eventId());if(e.traceId()!=null)MDC.put("eventTraceId",e.traceId());
        try{
            var span=Span.current();
            if(e.aggregateId()!=null)span.setAttribute("business.request.id",e.aggregateId());
            if(e.correlationId()!=null)span.setAttribute("business.correlation.id",e.correlationId());
            span.setAttribute("messaging.event.id",e.eventId());
            if(repo.existsByEventId(e.eventId()))return;
            var x=new AuditEventEntity();x.id=UUID.randomUUID();x.eventId=e.eventId();x.eventType=e.eventType();x.correlationId=e.correlationId();x.causationId=e.causationId();x.traceId=e.traceId();x.aggregateId=e.aggregateId();x.source=e.source();x.payloadJson=om.writeValueAsString(e.payload());x.occurredAt=e.occurredAt();x.recordedAt=Instant.now();repo.save(x);
        }finally{MDC.remove("requestId");MDC.remove("correlationId");MDC.remove("eventId");MDC.remove("eventTraceId");}
    }
}
