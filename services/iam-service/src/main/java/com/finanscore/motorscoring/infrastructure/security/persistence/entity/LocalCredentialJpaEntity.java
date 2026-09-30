package com.finanscore.motorscoring.infrastructure.security.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="credenciales_locales")
public class LocalCredentialJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="credencial_local_id") public Long id;
    @Column(name="usuario_app_id", nullable=false, unique=true) public Long userId;
    @Column(name="password_hash", nullable=false, length=500) public String passwordHash;
    @Column(name="failed_attempts", nullable=false) public int failedAttempts;
    @Column(name="locked_until") public Instant lockedUntil;
    @Column(name="password_changed_at", nullable=false) public Instant passwordChangedAt;
}
