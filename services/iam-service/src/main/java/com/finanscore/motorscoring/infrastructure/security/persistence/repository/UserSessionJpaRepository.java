package com.finanscore.motorscoring.infrastructure.security.persistence.repository;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.UserSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserSessionJpaRepository extends JpaRepository<UserSessionJpaEntity,Long> {
    Optional<UserSessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);
}
