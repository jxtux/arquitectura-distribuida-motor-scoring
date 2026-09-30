package com.finanscore.audit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DltEventRepository extends JpaRepository<DltEventEntity,UUID>{
    List<DltEventEntity> findTop500ByOrderByRecordedAtDesc();
    boolean existsByAggregateId(String aggregateId);
}
