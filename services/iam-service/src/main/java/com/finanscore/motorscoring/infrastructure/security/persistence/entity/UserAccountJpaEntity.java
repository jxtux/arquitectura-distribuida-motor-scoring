package com.finanscore.motorscoring.infrastructure.security.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="usuarios_app")
public class UserAccountJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="usuario_app_id") public Long id;
    @Column(name="display_name", nullable=false, length=120) public String displayName;
    @Column(name="email", unique=true, length=254) public String email;
    @Column(name="estado", nullable=false, length=30) public String status;
    @Column(name="email_verified", nullable=false) public boolean emailVerified;
    @Column(name="created_at", nullable=false) public Instant createdAt;
    @Column(name="last_login_at") public Instant lastLoginAt;
}
