package com.finanscore.motorscoring.infrastructure.security.persistence.repository;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.SocialIdentityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SocialIdentityJpaRepository extends JpaRepository<SocialIdentityJpaEntity,Long> {
    Optional<SocialIdentityJpaEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
