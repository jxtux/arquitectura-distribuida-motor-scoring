package com.finanscore.motorscoring.application.security.model;
public record ExternalIdentity(SocialProvider provider, String providerUserId, String email,
                               boolean emailVerified, String displayName) {}
