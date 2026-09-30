package com.finanscore.motorscoring.application.security.model;
import java.time.Instant;
public record MfaTotp(Long id, Long userId, String secretEncrypted, boolean enabled,
                      Instant confirmedAt, Instant createdAt) {}
