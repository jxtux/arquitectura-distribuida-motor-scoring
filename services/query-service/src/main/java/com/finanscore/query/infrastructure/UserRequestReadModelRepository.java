package com.finanscore.query.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRequestReadModelRepository extends JpaRepository<UserRequestReadModelEntity,UUID> {
    List<UserRequestReadModelEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserRequestReadModelEntity> findByRequestIdAndUserId(UUID requestId,Long userId);
}
