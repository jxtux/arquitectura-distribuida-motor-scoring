package com.finanscore.query.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedProjectionEventRepository extends JpaRepository<ProcessedProjectionEventEntity,String> {}
