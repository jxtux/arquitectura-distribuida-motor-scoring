package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.VerifyMfaCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.VerifyMfaUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

public final class VerifyMfaService implements VerifyMfaUseCase {
    private final TokenPort tokens;
    private final MfaTotpRepositoryPort mfa;
    private final SecretCipherPort cipher;
    private final TotpPort totp;
    private final AuthorizationRepositoryPort authorization;
    private final SessionRepositoryPort sessions;
    private final UserAccountRepositoryPort users;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public VerifyMfaService(TokenPort tokens, MfaTotpRepositoryPort mfa, SecretCipherPort cipher,
                            TotpPort totp, AuthorizationRepositoryPort authorization,
                            SessionRepositoryPort sessions, UserAccountRepositoryPort users,
                            Clock clock, Duration accessTtl, Duration refreshTtl) {
        this.tokens = tokens;
        this.mfa = mfa;
        this.cipher = cipher;
        this.totp = totp;
        this.authorization = authorization;
        this.sessions = sessions;
        this.users = users;
        this.clock = clock;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @Override
    public IssuedSession verify(VerifyMfaCommand c) {
        Long userId = tokens.validateTemporaryToken(c.challengeToken(), "MFA_CHALLENGE");
        UserAccount user = users.findById(userId)
            .orElseThrow(() -> new SecurityApplicationException("USER_NOT_FOUND", "Usuario no encontrado."));
        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        MfaTotp config = mfa.findByUserId(userId).filter(MfaTotp::enabled)
            .orElseThrow(() -> new SecurityApplicationException("MFA_NOT_ENABLED", "MFA no está habilitado."));
        if (!totp.verify(cipher.decrypt(config.secretEncrypted()), c.code())) {
            throw new SecurityApplicationException("INVALID_TOTP", "El código TOTP es incorrecto.");
        }

        IssuedSession issued = tokens.issueSession(
            userId, authorization.findRoles(userId), authorization.findPermissions(userId), accessTtl, refreshTtl);

        Instant now = clock.instant();
        sessions.save(new UserSession(
            null, userId, tokens.hashRefreshToken(issued.refreshToken()), c.ipAddress(), c.userAgent(),
            now, now.plus(refreshTtl), null));

        users.save(new UserAccount(
            user.id(), user.displayName(), user.email(), UserStatus.ACTIVE,
            user.emailVerified(), user.createdAt(), now));

        return issued;
    }
}
