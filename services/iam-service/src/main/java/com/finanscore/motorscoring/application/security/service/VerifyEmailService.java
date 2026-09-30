package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.VerifyEmailCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.VerifyEmailUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

public final class VerifyEmailService implements VerifyEmailUseCase {
    private final UserAccountRepositoryPort users;
    private final EmailVerificationRepositoryPort verifications;
    private final VerificationCodePort codes;
    private final MfaTotpRepositoryPort mfa;
    private final TokenPort tokens;
    private final Clock clock;

    public VerifyEmailService(UserAccountRepositoryPort users,
                              EmailVerificationRepositoryPort verifications,
                              VerificationCodePort codes,
                              MfaTotpRepositoryPort mfa,
                              TokenPort tokens,
                              Clock clock) {
        this.users = users;
        this.verifications = verifications;
        this.codes = codes;
        this.mfa = mfa;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public String verify(VerifyEmailCommand c) {
        UserAccount user = users.findByEmail(c.email().trim().toLowerCase())
            .orElseThrow(() -> new SecurityApplicationException("USER_NOT_FOUND", "Usuario no encontrado."));

        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        // Idempotencia: si el correo ya fue confirmado pero se perdió la navegación
        // al QR, se vuelve a emitir un setupToken sin pedir otro registro.
        if (user.emailVerified()) {
            if (mfa.findByUserId(user.id()).filter(MfaTotp::enabled).isPresent()) {
                throw new SecurityApplicationException(
                    "EMAIL_ALREADY_VERIFIED",
                    "El correo y Google Authenticator ya están configurados. Inicia sesión.");
            }
            ensureMfaSetupRequired(user);
            return tokens.issueTemporaryToken(user.id(), "MFA_SETUP", Duration.ofMinutes(10));
        }

        EmailVerification v = verifications.findLatestPendingByUserId(user.id())
            .orElseThrow(() -> new SecurityApplicationException("CODE_NOT_FOUND", "No existe un código pendiente."));

        Instant now = clock.instant();
        if (v.used() || now.isAfter(v.expiresAt())) {
            throw new SecurityApplicationException("CODE_EXPIRED", "El código expiró.");
        }
        if (v.attempts() >= 5) {
            throw new SecurityApplicationException("CODE_BLOCKED", "Se excedió el número de intentos.");
        }

        if (!codes.matches(c.code(), v.codeHash())) {
            verifications.save(new EmailVerification(
                v.id(), v.userId(), v.codeHash(), v.expiresAt(),
                v.attempts() + 1, v.usedAt(), v.createdAt()));
            throw new SecurityApplicationException("INVALID_CODE", "El código de verificación es incorrecto.");
        }

        verifications.save(new EmailVerification(
            v.id(), v.userId(), v.codeHash(), v.expiresAt(), v.attempts(), now, v.createdAt()));
        users.save(new UserAccount(
            user.id(), user.displayName(), user.email(), UserStatus.MFA_SETUP_REQUIRED,
            true, user.createdAt(), user.lastLoginAt()));

        return tokens.issueTemporaryToken(user.id(), "MFA_SETUP", Duration.ofMinutes(10));
    }

    private void ensureMfaSetupRequired(UserAccount user) {
        if (user.status() != UserStatus.MFA_SETUP_REQUIRED) {
            users.save(new UserAccount(
                user.id(), user.displayName(), user.email(), UserStatus.MFA_SETUP_REQUIRED,
                true, user.createdAt(), user.lastLoginAt()));
        }
    }
}
