package com.finanscore.query.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface OperationReadModelRepository extends JpaRepository<OperationReadModelEntity,UUID> {
    @Query(value="""
        SELECT * FROM operation_read_model o
        WHERE (:q IS NULL OR :q = '' OR
               CAST(o.request_id AS TEXT) ILIKE CONCAT('%',:q,'%') OR
               o.correlation_id ILIKE CONCAT('%',:q,'%') OR
               CAST(o.user_id AS TEXT) ILIKE CONCAT('%',:q,'%') OR
               COALESCE(o.product_code,'') ILIKE CONCAT('%',:q,'%'))
          AND (:status IS NULL OR :status = '' OR o.workflow_status = :status)
        ORDER BY o.updated_at DESC
        """,nativeQuery=true)
    List<OperationReadModelEntity> search(@Param("q") String query,@Param("status") String status,Pageable pageable);
}
