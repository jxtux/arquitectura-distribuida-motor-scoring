package com.finanscore.motorscoring.infrastructure.security.persistence.adapter;

import com.finanscore.motorscoring.application.security.model.MfaTotp;
import com.finanscore.motorscoring.application.security.port.out.MfaTotpRepositoryPort;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.MfaTotpJpaEntity;
import com.finanscore.motorscoring.infrastructure.security.persistence.repository.MfaTotpJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional
public class MfaTotpJpaAdapter implements MfaTotpRepositoryPort {

    private final MfaTotpJpaRepository mfaRepository;

    public MfaTotpJpaAdapter(MfaTotpJpaRepository mfaRepository) {
        this.mfaRepository = mfaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MfaTotp> findByUserId(Long userId) {
        return mfaRepository.findByUserId(userId).map(this::toModel);
    }

    @Override
    public MfaTotp save(MfaTotp mfa) {
        MfaTotpJpaEntity entity = mfa.id() == null
                ? new MfaTotpJpaEntity()
                : mfaRepository.findById(mfa.id()).orElse(new MfaTotpJpaEntity());
        entity.id = mfa.id();
        entity.userId = mfa.userId();
        entity.secretEncrypted = mfa.secretEncrypted();
        entity.enabled = mfa.enabled();
        entity.confirmedAt = mfa.confirmedAt();
        entity.createdAt = mfa.createdAt();
        return toModel(mfaRepository.saveAndFlush(entity));
    }

    private MfaTotp toModel(MfaTotpJpaEntity entity) {
        return new MfaTotp(entity.id, entity.userId, entity.secretEncrypted, entity.enabled,
                entity.confirmedAt, entity.createdAt);
    }
}
