package com.finanscore.support.observability;

import com.finanscore.contracts.EventEnvelope;
import io.opentelemetry.api.trace.Span;
import org.slf4j.MDC;

/** Contexto de negocio que complementa traceId/spanId de OpenTelemetry. */
public final class ObservabilityContext {
    private ObservabilityContext() {}

    public static Scope bind(String requestId, String correlationId, String eventId) {
        put("requestId", requestId);
        put("correlationId", correlationId);
        put("eventId", eventId);
        var span = Span.current();
        if (requestId != null) span.setAttribute("business.request.id", requestId);
        if (correlationId != null) span.setAttribute("business.correlation.id", correlationId);
        if (eventId != null) span.setAttribute("messaging.event.id", eventId);
        if (span.getSpanContext().isValid()) MDC.put("traceId", span.getSpanContext().getTraceId());
        return new Scope();
    }

    public static Scope bind(EventEnvelope e) {
        var scope = bind(e.aggregateId(), e.correlationId(), e.eventId());
        MDC.remove("eventTraceId");
        if (e.traceId() != null && !e.traceId().isBlank()) {
            MDC.put("eventTraceId", e.traceId());
            Span.current().setAttribute("messaging.source.trace.id", e.traceId());
        }
        return scope;
    }

    public static String currentTraceId() {
        var ctx = Span.current().getSpanContext();
        return ctx.isValid() ? ctx.getTraceId() : null;
    }

    private static void put(String key, String value) {
        if (value != null && !value.isBlank()) MDC.put(key, value);
    }

    public static final class Scope implements AutoCloseable {
        @Override public void close() {
            MDC.remove("requestId"); MDC.remove("correlationId"); MDC.remove("eventId"); MDC.remove("traceId"); MDC.remove("eventTraceId");
        }
    }
}
