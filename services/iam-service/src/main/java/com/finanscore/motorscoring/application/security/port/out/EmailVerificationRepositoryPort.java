package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.EmailVerification;
import java.util.Optional;
public interface EmailVerificationRepositoryPort {
    Optional<EmailVerification> findLatestPendingByUserId(Long userId);
    EmailVerification save(EmailVerification verification);
    void invalidatePendingByUserId(Long userId);
}
