package com.finanscore.motorscoring.bootstrap.security;

import com.finanscore.motorscoring.application.security.port.in.ProcessSocialIdentityUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
@Profile("api")
public class GoogleOAuthSecurityConfiguration {
    @Bean
    GoogleOidcSuccessHandler googleOidcSuccessHandler(ProcessSocialIdentityUseCase process,
        @Value("${iam.frontend.social-callback}") String frontendCallback){
        return new GoogleOidcSuccessHandler(process,frontendCallback);
    }
}
