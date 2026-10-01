package com.finanscore.support.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
	boolean existsByEventIdAndConsumerName(String eventId,String consumerName); }
