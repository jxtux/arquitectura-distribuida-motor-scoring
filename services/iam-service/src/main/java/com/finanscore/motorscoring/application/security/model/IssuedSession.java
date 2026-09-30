package com.finanscore.motorscoring.application.security.model;
public record IssuedSession(String accessToken, long accessExpiresInSeconds,
                            String refreshToken, long refreshExpiresInSeconds) {}
