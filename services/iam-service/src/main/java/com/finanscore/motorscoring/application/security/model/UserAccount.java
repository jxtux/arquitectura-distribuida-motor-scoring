package com.finanscore.motorscoring.application.security.model;
import java.time.Instant;
public record UserAccount(Long id, String displayName, String email, UserStatus status,
                          boolean emailVerified, Instant createdAt, Instant lastLoginAt) {}
