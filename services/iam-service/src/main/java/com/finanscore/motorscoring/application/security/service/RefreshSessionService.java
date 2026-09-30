package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.RefreshSessionUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

public final class RefreshSessionService implements RefreshSessionUseCase {
    private final TokenPort tokens;
    private final SessionRepositoryPort sessions;
    private final AuthorizationRepositoryPort authorization;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public RefreshSessionService(TokenPort tokens, SessionRepositoryPort sessions,
                                 AuthorizationRepositoryPort authorization, Clock clock,
                                 Duration accessTtl, Duration refreshTtl) {
        this.tokens = tokens; this.sessions = sessions; this.authorization = authorization;
        this.clock = clock; this.accessTtl = accessTtl; this.refreshTtl = refreshTtl;
    }

    @Override
    public IssuedSession refresh(String refreshToken, String ipAddress, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank())
            throw new SecurityApplicationException("REFRESH_TOKEN_REQUIRED", "Refresh token requerido.");

        UserSession old = sessions.findByRefreshTokenHash(tokens.hashRefreshToken(refreshToken))
            .orElseThrow(() -> new SecurityApplicationException("INVALID_REFRESH_TOKEN", "Refresh token inválido."));
        Instant now = clock.instant();
        if (old.revoked() || now.isAfter(old.expiresAt()))
            throw new SecurityApplicationException("REFRESH_TOKEN_EXPIRED", "La sesión expiró.");

        sessions.save(new UserSession(old.id(), old.userId(), old.refreshTokenHash(), old.ipAddress(), old.userAgent(),
                                      old.createdAt(), old.expiresAt(), now));

        IssuedSession issued = tokens.issueSession(
            old.userId(), authorization.findRoles(old.userId()), authorization.findPermissions(old.userId()),
            accessTtl, refreshTtl);

        sessions.save(new UserSession(
            null, old.userId(), tokens.hashRefreshToken(issued.refreshToken()), ipAddress, userAgent,
            now, now.plus(refreshTtl), null));

        return issued;
    }
}
