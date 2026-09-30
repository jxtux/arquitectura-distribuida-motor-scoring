package com.finanscore.motorscoring.application.security.model;
import java.time.Instant;
public record LocalCredential(Long id, Long userId, String passwordHash, int failedAttempts,
                              Instant lockedUntil, Instant passwordChangedAt) {}
