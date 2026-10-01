package com.finanscore.support.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;


//Busca eventos pendientes, los publica en Kafka y marca que fueron enviados. 
//“La publicación a Kafka queda desacoplada de la transacción de negocio.”


@Component
public class OutboxPublisher {
	private final OutboxEventRepository repo;
	private final KafkaTemplate<String, String> kafka;

	public OutboxPublisher(OutboxEventRepository r, KafkaTemplate<String, String> k) {
		repo = r;
		kafka = k;
	}

	@Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:1000}")
	@Transactional
	public void publish() {
		for (var x : repo.findTop100ByStatusOrderByCreatedAtAsc("PENDING")) {
			try {
				kafka.send(x.topic, x.eventKey, x.payload).get();
				x.status = "PUBLISHED";
				x.publishedAt = Instant.now();
			} catch (Exception e) {
				break;
			}
		}
	}
}
