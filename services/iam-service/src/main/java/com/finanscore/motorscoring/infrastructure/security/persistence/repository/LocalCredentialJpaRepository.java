package com.finanscore.motorscoring.infrastructure.security.persistence.repository;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.LocalCredentialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LocalCredentialJpaRepository extends JpaRepository<LocalCredentialJpaEntity,Long> {
    Optional<LocalCredentialJpaEntity> findByUserId(Long userId);
}
