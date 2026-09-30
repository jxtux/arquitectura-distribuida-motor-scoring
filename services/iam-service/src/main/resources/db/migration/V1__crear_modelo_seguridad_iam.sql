CREATE TABLE usuarios_app (
    usuario_app_id BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    email VARCHAR(254),
    estado VARCHAR(30) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    CONSTRAINT uk_usuarios_app_email UNIQUE (email)
);

CREATE TABLE credenciales_locales (
    credencial_local_id BIGSERIAL PRIMARY KEY,
    usuario_app_id BIGINT NOT NULL UNIQUE REFERENCES usuarios_app(usuario_app_id),
    password_hash VARCHAR(500) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE email_verification_code (
    email_verification_id BIGSERIAL PRIMARY KEY,
    usuario_app_id BIGINT NOT NULL REFERENCES usuarios_app(usuario_app_id),
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_email_verification_user
    ON email_verification_code(usuario_app_id, created_at DESC);

CREATE TABLE identidades_sociales (
    identidad_social_id BIGSERIAL PRIMARY KEY,
    usuario_app_id BIGINT NOT NULL REFERENCES usuarios_app(usuario_app_id),
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(254),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    CONSTRAINT uk_social_provider_user UNIQUE(provider, provider_user_id)
);
CREATE INDEX idx_social_user ON identidades_sociales(usuario_app_id);

CREATE TABLE mfa_totp (
    mfa_totp_id BIGSERIAL PRIMARY KEY,
    usuario_app_id BIGINT NOT NULL UNIQUE REFERENCES usuarios_app(usuario_app_id),
    secret_encrypted VARCHAR(1000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sesiones_usuario (
    sesion_usuario_id BIGSERIAL PRIMARY KEY,
    usuario_app_id BIGINT NOT NULL REFERENCES usuarios_app(usuario_app_id),
    refresh_token_hash VARCHAR(64) NOT NULL UNIQUE,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_sesion_user ON sesiones_usuario(usuario_app_id);
CREATE INDEX idx_sesion_active
    ON sesiones_usuario(usuario_app_id, expires_at) WHERE revoked_at IS NULL;

CREATE TABLE roles (
    rol_id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE permisos (
    permiso_id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE usuario_rol (
    usuario_rol_id BIGSERIAL PRIMARY KEY,
    usuario_app_id BIGINT NOT NULL REFERENCES usuarios_app(usuario_app_id),
    rol_id BIGINT NOT NULL REFERENCES roles(rol_id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_rol UNIQUE(usuario_app_id, rol_id)
);

CREATE TABLE rol_permiso (
    rol_permiso_id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL REFERENCES roles(rol_id),
    permiso_id BIGINT NOT NULL REFERENCES permisos(permiso_id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rol_permiso UNIQUE(rol_id, permiso_id)
);
