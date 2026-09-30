package com.finanscore.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanscore.contracts.EventEnvelope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;

@Component
public class DltAuditConsumer {
    private final ObjectMapper om;
    private final DltEventRepository repo;
    public DltAuditConsumer(ObjectMapper om, DltEventRepository repo){this.om=om;this.repo=repo;}

    @WithSpan("index-dlt-event")
    @KafkaListener(topicPattern=".*\\.DLT", groupId="audit-dlt-indexer")
    @Transactional
    public void on(ConsumerRecord<String,String> record){
        try {
            var x=new DltEventEntity();
            x.id=UUID.randomUUID(); x.sourceTopic=record.topic(); x.partition=record.partition(); x.offset=record.offset();
            x.payloadJson=record.value(); x.recordedAt=Instant.now();
            try {
                EventEnvelope e=om.readValue(record.value(),EventEnvelope.class);
                x.eventId=e.eventId();x.eventType=e.eventType();x.aggregateId=e.aggregateId();x.correlationId=e.correlationId();x.traceId=e.traceId();x.sourceService=e.source();
            } catch(Exception ignored) { }
            Map<String,String> headers=new LinkedHashMap<>();
            record.headers().forEach(h->{
                String value=h.value()==null?"":new String(h.value(), StandardCharsets.UTF_8);
                headers.put(h.key(),value);
                String key=h.key().toLowerCase(Locale.ROOT);
                if(key.contains("exception-message")) x.exceptionMessage=value;
                if(key.contains("delivery-attempt") && h.value()!=null){ try{x.retryCount=h.value().length==4?ByteBuffer.wrap(h.value()).getInt():Integer.valueOf(value.trim());}catch(Exception ignored){} }
            });
            x.headersJson=om.writeValueAsString(headers);
            repo.save(x);
        } catch(Exception ignored) {
            // El indexador de DLT nunca debe generar otro DLT por un fallo de observabilidad.
        }
    }
}
