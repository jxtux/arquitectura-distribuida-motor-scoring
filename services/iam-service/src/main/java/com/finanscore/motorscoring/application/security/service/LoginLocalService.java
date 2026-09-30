package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.LoginLocalCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.LoginLocalUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

public final class LoginLocalService implements LoginLocalUseCase {
    private final UserAccountRepositoryPort users;
    private final LocalCredentialRepositoryPort credentials;
    private final MfaTotpRepositoryPort mfa;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokens;
    private final Clock clock;

    public LoginLocalService(UserAccountRepositoryPort users,
                             LocalCredentialRepositoryPort credentials,
                             MfaTotpRepositoryPort mfa,
                             PasswordHasherPort passwordHasher,
                             TokenPort tokens,
                             Clock clock) {
        this.users = users;
        this.credentials = credentials;
        this.mfa = mfa;
        this.passwordHasher = passwordHasher;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public AuthStage login(LoginLocalCommand c) {
        UserAccount user = users.findByEmail(c.email().trim().toLowerCase())
            .orElseThrow(() -> new SecurityApplicationException("INVALID_CREDENTIALS", "Credenciales inválidas."));

        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        LocalCredential cred = credentials.findByUserId(user.id())
            .orElseThrow(() -> new SecurityApplicationException("LOCAL_LOGIN_NOT_AVAILABLE", "La cuenta no tiene contraseña local."));

        Instant now = clock.instant();
        if (cred.lockedUntil() != null && now.isBefore(cred.lockedUntil())) {
            throw new SecurityApplicationException("ACCOUNT_TEMPORARILY_LOCKED", "Cuenta bloqueada temporalmente.");
        }

        if (!passwordHasher.matches(c.password(), cred.passwordHash())) {
            int attempts = cred.failedAttempts() + 1;
            Instant lockUntil = attempts >= 5 ? now.plus(Duration.ofMinutes(15)) : null;
            credentials.save(new LocalCredential(
                cred.id(), cred.userId(), cred.passwordHash(), attempts, lockUntil, cred.passwordChangedAt()));
            throw new SecurityApplicationException("INVALID_CREDENTIALS", "Credenciales inválidas.");
        }

        credentials.save(new LocalCredential(
            cred.id(), cred.userId(), cred.passwordHash(), 0, null, cred.passwordChangedAt()));

        if (!user.emailVerified()) {
            throw new SecurityApplicationException(
                "EMAIL_VERIFICATION_REQUIRED",
                "Tu cuenta existe, pero falta validar el correo. Vuelve a Crear cuenta con el mismo correo y contraseña para recibir un código nuevo.");
        }

        boolean configured = mfa.findByUserId(user.id()).filter(MfaTotp::enabled).isPresent();
        if (!configured) {
            if (user.status() != UserStatus.MFA_SETUP_REQUIRED) {
                users.save(new UserAccount(
                    user.id(), user.displayName(), user.email(), UserStatus.MFA_SETUP_REQUIRED,
                    true, user.createdAt(), user.lastLoginAt()));
            }
            String setupToken = tokens.issueTemporaryToken(user.id(), "MFA_SETUP", Duration.ofMinutes(10));
            return new AuthStage(setupToken, "MFA_SETUP", false);
        }

        // Repara registros antiguos que pudieron quedar con MFA confirmado pero estado intermedio.
        if (user.status() != UserStatus.ACTIVE) {
            users.save(new UserAccount(
                user.id(), user.displayName(), user.email(), UserStatus.ACTIVE,
                true, user.createdAt(), user.lastLoginAt()));
        }

        String challenge = tokens.issueTemporaryToken(user.id(), "MFA_CHALLENGE", Duration.ofMinutes(5));
        return new AuthStage(challenge, "MFA_VERIFY", true);
    }
}
