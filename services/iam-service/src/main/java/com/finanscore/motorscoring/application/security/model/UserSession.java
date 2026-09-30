package com.finanscore.motorscoring.application.security.model;
import java.time.Instant;
public record UserSession(Long id, Long userId, String refreshTokenHash, String ipAddress,
                          String userAgent, Instant createdAt, Instant expiresAt, Instant revokedAt) {
    public boolean revoked() { return revokedAt != null; }
}
