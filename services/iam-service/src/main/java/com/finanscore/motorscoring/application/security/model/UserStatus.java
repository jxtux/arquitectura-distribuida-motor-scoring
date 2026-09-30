package com.finanscore.motorscoring.application.security.model;

/**
 * Estado de activación de la cuenta.
 *
 * PENDING_VERIFICATION: falta validar el correo (registro local).
 * MFA_SETUP_REQUIRED: identidad primaria validada, falta enlazar/confirmar TOTP.
 * ACTIVE: MFA confirmado; la cuenta puede completar autenticación.
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    MFA_SETUP_REQUIRED,
    ACTIVE,
    BLOCKED,
    DISABLED
}
