package com.finanscore.motorscoring.application.security.model;
import java.time.Instant;
public record EmailVerification(Long id, Long userId, String codeHash, Instant expiresAt,
                                int attempts, Instant usedAt, Instant createdAt) {
    public boolean used() { return usedAt != null; }
}
