package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.*;
public interface ExternalIdentityProviderPort {
    ExternalIdentity exchangeAuthorizationCode(SocialProvider provider, String code, String redirectUri);
}
