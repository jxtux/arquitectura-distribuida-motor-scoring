package com.finanscore.motorscoring.infrastructure.security.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="mfa_totp")
public class MfaTotpJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="mfa_totp_id") public Long id;
    @Column(name="usuario_app_id", nullable=false, unique=true) public Long userId;
    @Column(name="secret_encrypted", nullable=false, length=1000) public String secretEncrypted;
    @Column(name="enabled", nullable=false) public boolean enabled;
    @Column(name="confirmed_at") public Instant confirmedAt;
    @Column(name="created_at", nullable=false) public Instant createdAt;
}
