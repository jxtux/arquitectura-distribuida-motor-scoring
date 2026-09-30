package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.ConfirmMfaCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.ConfirmMfaUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

/**
 * Confirma el primer TOTP y completa el alta: MFA habilitado -> ACTIVE -> sesión/JWT.
 */
public final class ConfirmMfaService implements ConfirmMfaUseCase {
    private final TokenPort tokens;
    private final MfaTotpRepositoryPort mfa;
    private final TotpPort totp;
    private final SecretCipherPort cipher;
    private final AuthorizationRepositoryPort authorization;
    private final SessionRepositoryPort sessions;
    private final UserAccountRepositoryPort users;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public ConfirmMfaService(TokenPort tokens,
                             MfaTotpRepositoryPort mfa,
                             TotpPort totp,
                             SecretCipherPort cipher,
                             AuthorizationRepositoryPort authorization,
                             SessionRepositoryPort sessions,
                             UserAccountRepositoryPort users,
                             Clock clock,
                             Duration accessTtl,
                             Duration refreshTtl) {
        this.tokens = tokens;
        this.mfa = mfa;
        this.totp = totp;
        this.cipher = cipher;
        this.authorization = authorization;
        this.sessions = sessions;
        this.users = users;
        this.clock = clock;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @Override
    public IssuedSession confirm(ConfirmMfaCommand c) {
        Long userId = tokens.validateTemporaryToken(c.setupToken(), "MFA_SETUP");
        UserAccount user = users.findById(userId)
            .orElseThrow(() -> new SecurityApplicationException("USER_NOT_FOUND", "Usuario no encontrado."));

        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        MfaTotp current = mfa.findByUserId(userId)
            .orElseThrow(() -> new SecurityApplicationException("MFA_NOT_INITIALIZED", "MFA no fue configurado."));

        String secret = cipher.decrypt(current.secretEncrypted());
        if (!totp.verify(secret, c.code())) {
            throw new SecurityApplicationException("INVALID_TOTP", "El código TOTP es incorrecto.");
        }

        Instant now = clock.instant();
        if (!current.enabled()) {
            mfa.save(new MfaTotp(
                current.id(), current.userId(), current.secretEncrypted(), true, now, current.createdAt()));
        }

        users.save(new UserAccount(
            user.id(), user.displayName(), user.email(), UserStatus.ACTIVE,
            user.emailVerified(), user.createdAt(), now));

        IssuedSession issued = tokens.issueSession(
            userId,
            authorization.findRoles(userId),
            authorization.findPermissions(userId),
            accessTtl,
            refreshTtl);

        sessions.save(new UserSession(
            null, userId, tokens.hashRefreshToken(issued.refreshToken()),
            c.ipAddress(), c.userAgent(), now, now.plus(refreshTtl), null));

        return issued;
    }
}
