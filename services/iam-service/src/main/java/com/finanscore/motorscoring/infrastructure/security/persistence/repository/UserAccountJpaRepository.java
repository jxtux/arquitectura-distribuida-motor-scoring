package com.finanscore.motorscoring.infrastructure.security.persistence.repository;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.UserAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserAccountJpaRepository extends JpaRepository<UserAccountJpaEntity,Long> {
    Optional<UserAccountJpaEntity> findByEmailIgnoreCase(String email);
}
