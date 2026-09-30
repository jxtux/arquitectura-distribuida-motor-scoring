package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.model.*;
public interface LoginSocialUseCase {
    AuthStage login(SocialProvider provider, String authorizationCode, String redirectUri);
}
