package com.finanscore.motorscoring.infrastructure.security.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="email_verification_code")
public class EmailVerificationJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="email_verification_id") public Long id;
    @Column(name="usuario_app_id", nullable=false) public Long userId;
    @Column(name="code_hash", nullable=false, length=128) public String codeHash;
    @Column(name="expires_at", nullable=false) public Instant expiresAt;
    @Column(name="attempts", nullable=false) public int attempts;
    @Column(name="used_at") public Instant usedAt;
    @Column(name="created_at", nullable=false) public Instant createdAt;
}
