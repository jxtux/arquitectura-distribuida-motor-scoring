package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.ProcessSocialIdentityCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.ProcessSocialIdentityUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

/**
 * Unifica el post-login de Google OIDC y TikTok OAuth2.
 * Ambos proveedores terminan obligatoriamente en MFA_SETUP o MFA_VERIFY;
 * nunca emiten por sí solos el JWT final de Motor Scoring.
 */
public final class ProcessSocialIdentityService implements ProcessSocialIdentityUseCase {
    private final SocialIdentityRepositoryPort identities;
    private final UserAccountRepositoryPort users;
    private final AuthorizationRepositoryPort authorization;
    private final MfaTotpRepositoryPort mfa;
    private final TokenPort tokens;
    private final Clock clock;

    public ProcessSocialIdentityService(SocialIdentityRepositoryPort identities,
                                        UserAccountRepositoryPort users,
                                        AuthorizationRepositoryPort authorization,
                                        MfaTotpRepositoryPort mfa,
                                        TokenPort tokens,
                                        Clock clock) {
        this.identities = identities;
        this.users = users;
        this.authorization = authorization;
        this.mfa = mfa;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public AuthStage process(ProcessSocialIdentityCommand c) {
        ExternalIdentity ext = c.identity();
        Instant now = clock.instant();
        SocialIdentity identity = identities.find(ext.provider(), ext.providerUserId()).orElse(null);
        UserAccount user;

        if (identity != null) {
            identities.save(new SocialIdentity(
                identity.id(), identity.userId(), identity.provider(), identity.providerUserId(),
                identity.providerEmail(), identity.createdAt(), now));
            user = users.findById(identity.userId())
                .orElseThrow(() -> new SecurityApplicationException("USER_NOT_FOUND", "Usuario social no encontrado."));
        } else {
            String normalizedEmail = ext.email() == null ? null : ext.email().trim().toLowerCase();
            UserAccount existingByEmail = normalizedEmail == null
                ? null
                : users.findByEmail(normalizedEmail).orElse(null);

            if (existingByEmail != null) {
                /*
                 * FIXED14: si el proveedor OIDC demuestra un correo VERIFICADO que coincide
                 * exactamente con una cuenta local cuyo correo también fue verificado,
                 * vinculamos la identidad social con esa misma cuenta en vez de producir un
                 * Whitelabel 500/EXPLICIT_LINK_REQUIRED.
                 *
                 * Esto permite el flujo esperado:
                 * Google OAuth -> misma cuenta local -> MFA_SETUP/MFA_VERIFY -> JWT.
                 *
                 * Nunca se vincula automáticamente si el proveedor no asegura el correo o si
                 * la cuenta local todavía no verificó ese correo.
                 */
                if (!ext.emailVerified() || !existingByEmail.emailVerified()) {
                    throw new SecurityApplicationException(
                        "EXPLICIT_LINK_REQUIRED",
                        "Ya existe una cuenta con ese correo, pero debe verificarse antes de vincular el acceso social.");
                }
                user = existingByEmail;
                identities.save(new SocialIdentity(
                    null, user.id(), ext.provider(), ext.providerUserId(), normalizedEmail, now, now));
            } else {
                user = users.save(new UserAccount(
                    null,
                    safeDisplayName(ext),
                    normalizedEmail,
                    UserStatus.MFA_SETUP_REQUIRED,
                    ext.emailVerified(),
                    now,
                    null));

                identities.save(new SocialIdentity(
                    null, user.id(), ext.provider(), ext.providerUserId(), normalizedEmail, now, now));
                authorization.assignRole(user.id(), "USER");
            }
        }

        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        boolean configured = mfa.findByUserId(user.id()).filter(MfaTotp::enabled).isPresent();
        if (!configured) {
            if (user.status() != UserStatus.MFA_SETUP_REQUIRED) {
                users.save(new UserAccount(
                    user.id(), user.displayName(), user.email(), UserStatus.MFA_SETUP_REQUIRED,
                    user.emailVerified(), user.createdAt(), user.lastLoginAt()));
            }
            String token = tokens.issueTemporaryToken(user.id(), "MFA_SETUP", Duration.ofMinutes(10));
            return new AuthStage(token, "MFA_SETUP", false);
        }

        if (user.status() != UserStatus.ACTIVE) {
            users.save(new UserAccount(
                user.id(), user.displayName(), user.email(), UserStatus.ACTIVE,
                user.emailVerified(), user.createdAt(), user.lastLoginAt()));
        }

        String token = tokens.issueTemporaryToken(user.id(), "MFA_CHALLENGE", Duration.ofMinutes(5));
        return new AuthStage(token, "MFA_VERIFY", true);
    }

    private static String safeDisplayName(ExternalIdentity ext) {
        if (ext.displayName() != null && !ext.displayName().isBlank()) {
            return ext.displayName().trim();
        }
        if (ext.email() != null && !ext.email().isBlank()) {
            return ext.email().trim();
        }
        return ext.provider().name() + " user";
    }
}
