package com.finanscore.motorscoring.infrastructure.security.persistence.repository;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.EmailVerificationJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationJpaEntity,Long> {
    Optional<EmailVerificationJpaEntity> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);
    @Modifying
    @Query("update EmailVerificationJpaEntity e set e.usedAt = CURRENT_TIMESTAMP where e.userId=:userId and e.usedAt is null")
    void invalidatePending(@Param("userId") Long userId);
}
