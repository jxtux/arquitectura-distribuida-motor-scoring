package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.SetupMfaUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

/**
 * Configuración TOTP idempotente mientras MFA aún no ha sido confirmado.
 *
 * FIXED11:
 * - si quedó un MFA incompleto/corrupto de una ejecución anterior, se rota SOLO
 *   el secreto no confirmado y se permite continuar con un QR nuevo;
 * - los errores del token temporal se convierten en errores IAM claros;
 * - nunca se rota automáticamente un MFA ya confirmado.
 */
public final class SetupMfaService implements SetupMfaUseCase {
    private final TokenPort tokens;
    private final UserAccountRepositoryPort users;
    private final MfaTotpRepositoryPort mfa;
    private final TotpPort totp;
    private final SecretCipherPort cipher;
    private final Clock clock;
    private final String issuer;

    public SetupMfaService(TokenPort tokens, UserAccountRepositoryPort users,
                           MfaTotpRepositoryPort mfa, TotpPort totp,
                           SecretCipherPort cipher, Clock clock, String issuer) {
        this.tokens = tokens;
        this.users = users;
        this.mfa = mfa;
        this.totp = totp;
        this.cipher = cipher;
        this.clock = clock;
        this.issuer = issuer;
    }

    @Override
    public MfaSetup setup(String setupToken) {
        final Long userId;
        try {
            userId = tokens.validateTemporaryToken(setupToken, "MFA_SETUP");
        } catch (RuntimeException ex) {
            throw new SecurityApplicationException(
                "INVALID_MFA_SETUP_TOKEN",
                "La sesión para configurar Google Authenticator no es válida o expiró. Vuelve a iniciar sesión o reanuda el registro.");
        }

        UserAccount user = users.findById(userId)
            .orElseThrow(() -> new SecurityApplicationException("USER_NOT_FOUND", "Usuario no encontrado."));

        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        MfaTotp current = mfa.findByUserId(userId).orElse(null);
        if (current != null && current.enabled()) {
            throw new SecurityApplicationException("MFA_ALREADY_ENABLED", "Google Authenticator ya está configurado.");
        }

        final String secret;
        if (current == null) {
            secret = createPendingSecret(userId, null);
        } else {
            secret = recoverOrRotatePendingSecret(current);
        }

        if (user.status() != UserStatus.MFA_SETUP_REQUIRED) {
            users.save(new UserAccount(
                user.id(), user.displayName(), user.email(), UserStatus.MFA_SETUP_REQUIRED,
                user.emailVerified(), user.createdAt(), user.lastLoginAt()));
        }

        String label = user.email() != null ? user.email() : "usuario-" + userId;
        return new MfaSetup(secret, totp.buildOtpAuthUri(issuer, label, secret));
    }

    private String recoverOrRotatePendingSecret(MfaTotp current) {
        try {
            String decrypted = cipher.decrypt(current.secretEncrypted());
            if (decrypted == null || decrypted.isBlank()) {
                return createPendingSecret(current.userId(), current);
            }
            return decrypted;
        } catch (RuntimeException ex) {
            /*
             * Es seguro rotar solamente porque current.enabled() == false.
             * Esto recupera registros a medio configurar después de cambiar la
             * IAM_TOTP_AES_KEY durante desarrollo, sin tocar MFA ya confirmado.
             */
            return createPendingSecret(current.userId(), current);
        }
    }

    private String createPendingSecret(Long userId, MfaTotp current) {
        String secret = totp.generateSecret();
        Instant now = clock.instant();
        Long id = current == null ? null : current.id();
        Instant createdAt = current == null || current.createdAt() == null ? now : current.createdAt();
        mfa.save(new MfaTotp(id, userId, cipher.encrypt(secret), false, null, createdAt));
        return secret;
    }
}
