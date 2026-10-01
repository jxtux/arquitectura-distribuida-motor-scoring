package com.finanscore.query.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanscore.contracts.*;
import com.finanscore.query.application.ProjectionService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class QueryProjectionConsumer {
	private static final Logger log = LoggerFactory.getLogger(QueryProjectionConsumer.class);
	private final ObjectMapper om;
	private final ProjectionService projections;

	public QueryProjectionConsumer(ObjectMapper om, ProjectionService projections) {
		this.om = om;
		this.projections = projections;
	}

	@WithSpan("consume-query-projection")
	@KafkaListener(topics = { EventTopics.CREDIT_REQUEST_CREATED, EventTopics.PAYMENT_VALIDATED,
			EventTopics.PAYMENT_REJECTED, EventTopics.CREDIT_EVALUATION_REQUESTED, EventTopics.SCORING_CALCULATED,
			EventTopics.SCORING_FAILED, EventTopics.REPORT_GENERATED, EventTopics.REPORT_FAILED,
			EventTopics.NOTIFICATION_SENT, EventTopics.NOTIFICATION_FAILED }, groupId = "query-service-projections")
	public void onBusinessEvent(String raw) throws Exception {
		EventEnvelope e = om.readValue(raw, EventEnvelope.class);
		withMdc(e, () -> projections.project(e));
	}

	//Query Service consume eventos y construye un modelo optimizado para lectura.
	//“Separamos el modelo de escritura del modelo utilizado para consultas como Mis solicitudes.”
	@WithSpan("consume-query-projection-dlt")
	@KafkaListener(topics = { 
			EventTopics.CREDIT_REQUEST_CREATED + ".DLT", EventTopics.PAYMENT_VALIDATED + ".DLT",
			EventTopics.PAYMENT_REJECTED + ".DLT", EventTopics.CREDIT_EVALUATION_REQUESTED + ".DLT",
			EventTopics.SCORING_CALCULATED + ".DLT", EventTopics.SCORING_FAILED + ".DLT",
			EventTopics.REPORT_GENERATED + ".DLT", EventTopics.REPORT_FAILED + ".DLT",
			EventTopics.NOTIFICATION_SENT + ".DLT",
			EventTopics.NOTIFICATION_FAILED + ".DLT" }, groupId = "query-service-projections-dlt")
	public void onDlt(String raw, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
		EventEnvelope e = om.readValue(raw, EventEnvelope.class);
		withMdc(e, () -> projections.projectDlt(e, topic));
	}

	private void withMdc(EventEnvelope e, ThrowingRunnable action) throws Exception {
		try (MDC.MDCCloseable a = MDC.putCloseable("requestId", e.aggregateId());
				MDC.MDCCloseable b = MDC.putCloseable("correlationId", e.correlationId());
				MDC.MDCCloseable c = MDC.putCloseable("eventId", e.eventId())) {
			action.run();
			log.info("Read model projected eventType={}", e.eventType());
		}
	}

	@FunctionalInterface
	interface ThrowingRunnable {
		void run() throws Exception;
	}
}
