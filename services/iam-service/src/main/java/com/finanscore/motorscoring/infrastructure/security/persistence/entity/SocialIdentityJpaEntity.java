package com.finanscore.motorscoring.infrastructure.security.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="identidades_sociales",
 uniqueConstraints=@UniqueConstraint(name="uk_social_provider_user", columnNames={"provider","provider_user_id"}))
public class SocialIdentityJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="identidad_social_id") public Long id;
    @Column(name="usuario_app_id", nullable=false) public Long userId;
    @Column(name="provider", nullable=false, length=30) public String provider;
    @Column(name="provider_user_id", nullable=false, length=255) public String providerUserId;
    @Column(name="provider_email", length=254) public String providerEmail;
    @Column(name="created_at", nullable=false) public Instant createdAt;
    @Column(name="last_login_at") public Instant lastLoginAt;
}
