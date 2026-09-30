package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.*;
import java.util.Optional;
public interface SocialIdentityRepositoryPort {
    Optional<SocialIdentity> find(SocialProvider provider, String providerUserId);
    SocialIdentity save(SocialIdentity identity);
}
