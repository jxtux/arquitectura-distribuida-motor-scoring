package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.model.UserSession;
import com.finanscore.motorscoring.application.security.port.in.LogoutUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.Clock;

public final class LogoutService implements LogoutUseCase {
    private final TokenPort tokens;
    private final SessionRepositoryPort sessions;
    private final Clock clock;

    public LogoutService(TokenPort tokens, SessionRepositoryPort sessions, Clock clock) {
        this.tokens = tokens; this.sessions = sessions; this.clock = clock;
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        sessions.findByRefreshTokenHash(tokens.hashRefreshToken(refreshToken)).ifPresent(s ->
            sessions.save(new UserSession(
                s.id(), s.userId(), s.refreshTokenHash(), s.ipAddress(), s.userAgent(),
                s.createdAt(), s.expiresAt(), clock.instant())));
    }
}
