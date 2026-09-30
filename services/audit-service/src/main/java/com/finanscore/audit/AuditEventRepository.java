package com.finanscore.audit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuditEventRepository extends JpaRepository<AuditEventEntity,UUID>{
    boolean existsByEventId(String id);
    List<AuditEventEntity> findByCorrelationIdOrderByOccurredAtAsc(String c);
    List<AuditEventEntity> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);
    List<AuditEventEntity> findTop2000ByOrderByOccurredAtDesc();
}
