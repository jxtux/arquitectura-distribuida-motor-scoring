package com.finanscore.scoring.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanscore.contracts.*;
import com.finanscore.motorscoring.application.command.*;
import com.finanscore.motorscoring.application.usecase.*;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.scoring.infrastructure.*;
import com.finanscore.scoring.application.CreditDataProviderPort;
import com.finanscore.support.idempotency.*;
import com.finanscore.support.observability.ObservabilityContext;
import com.finanscore.support.outbox.*;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Component
public class ScoringConsumer {
	private static final Logger log = LoggerFactory.getLogger(ScoringConsumer.class);
	private final ObjectMapper om;
	private final CreditDataProviderPort profiles;
	private final EvaluationInputSnapshotRepository snaps;
	private final CrearSolicitudCreditoUseCase create;
	private final EjecutarEvaluacionScoringUseCase eval;
	private final IdempotentEventGuard guard;
	private final OutboxService outbox;

	public ScoringConsumer(ObjectMapper o, CreditDataProviderPort p, EvaluationInputSnapshotRepository s,
			CrearSolicitudCreditoUseCase c, EjecutarEvaluacionScoringUseCase e, IdempotentEventGuard g,
			OutboxService b) {
		om = o;
		profiles = p;
		snaps = s;
		create = c;
		eval = e;
		guard = g;
		outbox = b;
	}

	//Scoring reacciona a un evento y publica el siguiente sin orquestador central. 
	//“Cada microservicio conoce su paso y la Saga avanza mediante eventos.”
	
	@WithSpan("calculate-scoring")
	@KafkaListener(topics = EventTopics.CREDIT_EVALUATION_REQUESTED, groupId = "scoring-calculation")
	@Transactional
	public void on(String raw) throws Exception {
		var ev = om.readValue(raw, EventEnvelope.class);
		try (var ignored = ObservabilityContext.bind(ev)) {
			if (guard.alreadyProcessed(ev.eventId(), "scoring-calculation"))
				return;
			var p = ev.payload();
			UUID requestId = UUID.fromString(String.valueOf(p.get("requestId")));
			Long userId = Long.valueOf(String.valueOf(p.get("userId")));
			var prof = profiles.get(userId);
			String product = String.valueOf(p.get("productCode"));
			BigDecimal amount = new BigDecimal(String.valueOf(p.get("amount")));
			int term = Integer.parseInt(String.valueOf(p.get("termMonths")));
			String purpose = String.valueOf(p.get("purpose"));
			var dto = create.ejecutar(
					new CrearSolicitudCreditoCommand(requestId.toString(), TipoDocumento.DNI, prof.documentNumber(),
							prof.displayName(), prof.monthlyIncome(), prof.monthlyExpenses(), prof.monthlyObligations(),
							prof.employmentMonths(), prof.activeObligations(), prof.paymentHistoryScore(),
							prof.delinquencyAlerts(), product, amount, term, Moneda.PEN, purpose, "DISTRIBUTED_KAFKA"));
			if (!snaps.existsByRequestId(requestId)) {
				var s = new EvaluationInputSnapshotEntity();
				s.id = UUID.randomUUID();
				s.requestId = requestId;
				s.userId = userId;
				s.inputJson = om.writeValueAsString(Map.of("profile", prof, "request", p));
				s.createdAt = Instant.now();
				snaps.save(s);
			}
			var r = eval.ejecutar(new EjecutarEvaluacionScoringCommand(dto.idSolicitud()));
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("requestId", requestId.toString());
			out.put("userId", userId);
			out.put("productCode", product);
			out.put("amount", amount);
			out.put("currency", "PEN");
			out.put("termMonths", term);
			out.put("purpose", purpose);
			out.put("score", r.puntajeTotal());
			out.put("recommendation", r.resultado());
			out.put("modelVersion", r.versionModelo());
			out.put("factors", r.factores());
			outbox.append("ScoringCalculated", requestId.toString(), ev.correlationId(), ev.eventId(), null, "scoring-service", out);
			
			log.info("Scoring calculated score={} recommendation={} modelVersion={}", r.puntajeTotal(), r.resultado(),r.versionModelo());
			
			guard.markProcessed(ev.eventId(), "scoring-calculation");
		}
	}
}
