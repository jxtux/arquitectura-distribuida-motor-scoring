package com.finanscore.motorscoring.infrastructure.security.persistence.repository;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.MfaTotpJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MfaTotpJpaRepository extends JpaRepository<MfaTotpJpaEntity,Long> {
    Optional<MfaTotpJpaEntity> findByUserId(Long userId);
}
