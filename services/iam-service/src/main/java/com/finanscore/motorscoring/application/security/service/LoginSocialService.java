package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.command.ProcessSocialIdentityCommand;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.*;
import com.finanscore.motorscoring.application.security.port.out.ExternalIdentityProviderPort;

public final class LoginSocialService implements LoginSocialUseCase {
    private final ExternalIdentityProviderPort provider;
    private final ProcessSocialIdentityUseCase process;

    public LoginSocialService(ExternalIdentityProviderPort provider, ProcessSocialIdentityUseCase process) {
        this.provider = provider; this.process = process;
    }

    @Override
    public AuthStage login(SocialProvider p, String code, String redirectUri) {
        return process.process(new ProcessSocialIdentityCommand(
            provider.exchangeAuthorizationCode(p, code, redirectUri)));
    }
}
