package com.finanscore.motorscoring.application.security.model;
import java.time.Instant;
public record SocialIdentity(Long id, Long userId, SocialProvider provider, String providerUserId,
                             String providerEmail, Instant createdAt, Instant lastLoginAt) {}
