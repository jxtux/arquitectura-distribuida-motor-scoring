package com.finanscore.motorscoring.infrastructure.security.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="sesiones_usuario")
public class UserSessionJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="sesion_usuario_id") public Long id;
    @Column(name="usuario_app_id", nullable=false) public Long userId;
    @Column(name="refresh_token_hash", nullable=false, unique=true, length=64) public String refreshTokenHash;
    @Column(name="ip_address", length=64) public String ipAddress;
    @Column(name="user_agent", length=500) public String userAgent;
    @Column(name="created_at", nullable=false) public Instant createdAt;
    @Column(name="expires_at", nullable=false) public Instant expiresAt;
    @Column(name="revoked_at") public Instant revokedAt;
}
