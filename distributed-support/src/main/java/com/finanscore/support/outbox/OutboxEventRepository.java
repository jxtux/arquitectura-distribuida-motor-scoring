package com.finanscore.support.outbox; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity,String>{ List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(String status); }
