package com.finanscore.motorscoring.infrastructure.security.persistence.adapter;

import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.out.*;
import com.finanscore.motorscoring.infrastructure.security.persistence.entity.*;
import com.finanscore.motorscoring.infrastructure.security.persistence.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
@Transactional
public class IamJpaAdapter implements UserAccountRepositoryPort, EmailVerificationRepositoryPort,
    SessionRepositoryPort, SocialIdentityRepositoryPort, AuthorizationRepositoryPort {

    private final UserAccountJpaRepository users;
    private final EmailVerificationJpaRepository verifications;
    private final UserSessionJpaRepository sessions;
    private final SocialIdentityJpaRepository identities;
    private final AuthorityJpaRepository authorities;

    public IamJpaAdapter(UserAccountJpaRepository users, EmailVerificationJpaRepository verifications,
                         UserSessionJpaRepository sessions, SocialIdentityJpaRepository identities,
                         AuthorityJpaRepository authorities) {
        this.users=users; this.verifications=verifications;
        this.sessions=sessions; this.identities=identities; this.authorities=authorities;
    }

    public Optional<UserAccount> findById(Long id){ return users.findById(id).map(this::toUser); }
    public Optional<UserAccount> findByEmail(String email){ return users.findByEmailIgnoreCase(email).map(this::toUser); }

    public UserAccount save(UserAccount x){
        UserAccountJpaEntity e=x.id()==null?new UserAccountJpaEntity():users.findById(x.id()).orElse(new UserAccountJpaEntity());
        e.id=x.id(); e.displayName=x.displayName(); e.email=x.email(); e.status=x.status().name();
        e.emailVerified=x.emailVerified(); e.createdAt=x.createdAt(); e.lastLoginAt=x.lastLoginAt();
        return toUser(users.save(e));
    }


    public Optional<EmailVerification> findLatestPendingByUserId(Long id){
        return verifications.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(id).map(this::toVerification);
    }
    public EmailVerification save(EmailVerification x){
        EmailVerificationJpaEntity e=x.id()==null?new EmailVerificationJpaEntity():verifications.findById(x.id()).orElse(new EmailVerificationJpaEntity());
        e.id=x.id(); e.userId=x.userId(); e.codeHash=x.codeHash(); e.expiresAt=x.expiresAt();
        e.attempts=x.attempts(); e.usedAt=x.usedAt(); e.createdAt=x.createdAt();
        return toVerification(verifications.save(e));
    }
    public void invalidatePendingByUserId(Long id){ verifications.invalidatePending(id); }


    public UserSession save(UserSession x){
        UserSessionJpaEntity e=x.id()==null?new UserSessionJpaEntity():sessions.findById(x.id()).orElse(new UserSessionJpaEntity());
        e.id=x.id(); e.userId=x.userId(); e.refreshTokenHash=x.refreshTokenHash(); e.ipAddress=x.ipAddress();
        e.userAgent=x.userAgent(); e.createdAt=x.createdAt(); e.expiresAt=x.expiresAt(); e.revokedAt=x.revokedAt();
        return toSession(sessions.save(e));
    }
    public Optional<UserSession> findByRefreshTokenHash(String h){ return sessions.findByRefreshTokenHash(h).map(this::toSession); }

    public Optional<SocialIdentity> find(SocialProvider p,String id){
        return identities.findByProviderAndProviderUserId(p.name(),id).map(this::toIdentity);
    }
    public SocialIdentity save(SocialIdentity x){
        SocialIdentityJpaEntity e=x.id()==null?new SocialIdentityJpaEntity():identities.findById(x.id()).orElse(new SocialIdentityJpaEntity());
        e.id=x.id(); e.userId=x.userId(); e.provider=x.provider().name(); e.providerUserId=x.providerUserId();
        e.providerEmail=x.providerEmail(); e.createdAt=x.createdAt(); e.lastLoginAt=x.lastLoginAt();
        return toIdentity(identities.save(e));
    }

    public Set<String> findRoles(Long id){ return new HashSet<>(authorities.findRoles(id)); }
    public Set<String> findPermissions(Long id){ return new HashSet<>(authorities.findPermissions(id)); }
    public void assignRole(Long id,String role){ authorities.assignRole(id,role); }

    private UserAccount toUser(UserAccountJpaEntity e){return new UserAccount(e.id,e.displayName,e.email,UserStatus.valueOf(e.status),e.emailVerified,e.createdAt,e.lastLoginAt);}
    private EmailVerification toVerification(EmailVerificationJpaEntity e){return new EmailVerification(e.id,e.userId,e.codeHash,e.expiresAt,e.attempts,e.usedAt,e.createdAt);}
    private UserSession toSession(UserSessionJpaEntity e){return new UserSession(e.id,e.userId,e.refreshTokenHash,e.ipAddress,e.userAgent,e.createdAt,e.expiresAt,e.revokedAt);}
    private SocialIdentity toIdentity(SocialIdentityJpaEntity e){return new SocialIdentity(e.id,e.userId,SocialProvider.valueOf(e.provider),e.providerUserId,e.providerEmail,e.createdAt,e.lastLoginAt);}
}
