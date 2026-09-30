package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.RegisterUserCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.RegisterUserUseCase;
import com.finanscore.motorscoring.application.security.port.out.*;
import java.time.*;

/**
 * Registro local tolerante a interrupciones.
 *
 * Si el mismo usuario quedó a medias, la combinación correo + contraseña permite
 * reanudar el flujo sin crear duplicados:
 * - correo pendiente -> genera un nuevo código de verificación;
 * - correo verificado y MFA pendiente -> entrega un nuevo setupToken;
 * - MFA ya confirmado -> informa que la cuenta ya existe.
 */
public final class RegisterUserService implements RegisterUserUseCase {
    private final UserAccountRepositoryPort users;
    private final LocalCredentialRepositoryPort credentials;
    private final EmailVerificationRepositoryPort verifications;
    private final AuthorizationRepositoryPort authorization;
    private final MfaTotpRepositoryPort mfa;
    private final PasswordHasherPort passwordHasher;
    private final VerificationCodePort codes;
    private final EmailSenderPort emailSender;
    private final TokenPort tokens;
    private final Clock clock;

    public RegisterUserService(UserAccountRepositoryPort users,
                               LocalCredentialRepositoryPort credentials,
                               EmailVerificationRepositoryPort verifications,
                               AuthorizationRepositoryPort authorization,
                               MfaTotpRepositoryPort mfa,
                               PasswordHasherPort passwordHasher,
                               VerificationCodePort codes,
                               EmailSenderPort emailSender,
                               TokenPort tokens,
                               Clock clock) {
        this.users = users;
        this.credentials = credentials;
        this.verifications = verifications;
        this.authorization = authorization;
        this.mfa = mfa;
        this.passwordHasher = passwordHasher;
        this.codes = codes;
        this.emailSender = emailSender;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public RegistrationStage register(RegisterUserCommand c) {
        String email = normalize(c.email());
        validateInput(c);

        UserAccount existing = users.findByEmail(email).orElse(null);
        if (existing != null) {
            return resumeExisting(existing, c.password());
        }

        Instant now = clock.instant();
        UserAccount user = users.save(new UserAccount(
            null, c.displayName().trim(), email,
            UserStatus.PENDING_VERIFICATION, false, now, null));

        credentials.save(new LocalCredential(
            null, user.id(), passwordHasher.hash(c.password()), 0, null, now));
        authorization.assignRole(user.id(), "USER");

        sendVerificationCode(user, now);
        return new RegistrationStage(
            "EMAIL_VERIFY", null,
            "Usuario creado. Revisa tu correo para continuar.");
    }

    private RegistrationStage resumeExisting(UserAccount user, String rawPassword) {
        if (user.status() == UserStatus.BLOCKED || user.status() == UserStatus.DISABLED) {
            throw new SecurityApplicationException("ACCOUNT_NOT_AVAILABLE", "La cuenta no está disponible.");
        }

        LocalCredential credential = credentials.findByUserId(user.id())
            .orElseThrow(() -> new SecurityApplicationException(
                "EMAIL_ALREADY_EXISTS",
                "Ese correo pertenece a una cuenta social. Inicia sesión con su proveedor."));

        if (!passwordHasher.matches(rawPassword, credential.passwordHash())) {
            throw new SecurityApplicationException(
                "EMAIL_ALREADY_EXISTS",
                "Ya existe una cuenta con ese correo. Usa la contraseña de esa cuenta para reanudar el registro.");
        }

        Instant now = clock.instant();
        if (!user.emailVerified()) {
            sendVerificationCode(user, now);
            return new RegistrationStage(
                "EMAIL_VERIFY", null,
                "La cuenta ya existía pero faltaba validar el correo. Enviamos un código nuevo.");
        }

        boolean mfaEnabled = mfa.findByUserId(user.id()).filter(MfaTotp::enabled).isPresent();
        if (mfaEnabled) {
            throw new SecurityApplicationException(
                "EMAIL_ALREADY_EXISTS",
                "La cuenta ya está registrada. Inicia sesión.");
        }

        if (user.status() != UserStatus.MFA_SETUP_REQUIRED) {
            users.save(new UserAccount(
                user.id(), user.displayName(), user.email(), UserStatus.MFA_SETUP_REQUIRED,
                true, user.createdAt(), user.lastLoginAt()));
        }

        String setupToken = tokens.issueTemporaryToken(user.id(), "MFA_SETUP", Duration.ofMinutes(10));
        return new RegistrationStage(
            "MFA_SETUP", setupToken,
            "El correo ya estaba verificado. Continúa configurando Google Authenticator.");
    }

    private void sendVerificationCode(UserAccount user, Instant now) {
        verifications.invalidatePendingByUserId(user.id());
        String code = codes.generate();
        verifications.save(new EmailVerification(
            null, user.id(), codes.hash(code), now.plus(Duration.ofMinutes(10)), 0, null, now));
        emailSender.sendEmailVerificationCode(user.email(), user.displayName(), code);
    }

    private void validateInput(RegisterUserCommand c) {
        if (c.displayName() == null || c.displayName().isBlank()) {
            throw new SecurityApplicationException("INVALID_NAME", "El nombre es obligatorio.");
        }
        if (c.password() == null || c.password().length() < 12) {
            throw new SecurityApplicationException("WEAK_PASSWORD", "La contraseña debe tener al menos 12 caracteres.");
        }
    }

    private String normalize(String email) {
        if (email == null || !email.contains("@")) {
            throw new SecurityApplicationException("INVALID_EMAIL", "Ingrese un correo válido.");
        }
        return email.trim().toLowerCase();
    }
}
