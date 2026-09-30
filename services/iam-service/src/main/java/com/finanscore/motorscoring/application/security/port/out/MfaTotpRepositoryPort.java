package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.MfaTotp;
import java.util.Optional;
public interface MfaTotpRepositoryPort {
    Optional<MfaTotp> findByUserId(Long userId);
    MfaTotp save(MfaTotp mfa);
}
