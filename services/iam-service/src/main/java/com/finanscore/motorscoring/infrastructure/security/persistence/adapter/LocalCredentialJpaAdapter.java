package com.finanscore.motorscoring.infrastructure.security.persistence.adapter;

import com.finanscore.motorscoring.application.security.model.LocalCredential;
import com.finanscore.motorscoring.application.security.port.out.LocalCredentialRepositoryPort;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.LocalCredentialJpaEntity;
import com.finanscore.motorscoring.infrastructure.security.persistence.repository.LocalCredentialJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional
public class LocalCredentialJpaAdapter implements LocalCredentialRepositoryPort {

    private final LocalCredentialJpaRepository credentials;

    public LocalCredentialJpaAdapter(LocalCredentialJpaRepository credentials) {
        this.credentials = credentials;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalCredential> findByUserId(Long userId) {
        return credentials.findByUserId(userId).map(this::toModel);
    }

    @Override
    public LocalCredential save(LocalCredential credential) {
        LocalCredentialJpaEntity entity = credential.id() == null
                ? new LocalCredentialJpaEntity()
                : credentials.findById(credential.id()).orElse(new LocalCredentialJpaEntity());
        entity.id = credential.id();
        entity.userId = credential.userId();
        entity.passwordHash = credential.passwordHash();
        entity.failedAttempts = credential.failedAttempts();
        entity.lockedUntil = credential.lockedUntil();
        entity.passwordChangedAt = credential.passwordChangedAt();
        return toModel(credentials.save(entity));
    }

    private LocalCredential toModel(LocalCredentialJpaEntity entity) {
        return new LocalCredential(entity.id, entity.userId, entity.passwordHash, entity.failedAttempts,
                entity.lockedUntil, entity.passwordChangedAt);
    }
}
