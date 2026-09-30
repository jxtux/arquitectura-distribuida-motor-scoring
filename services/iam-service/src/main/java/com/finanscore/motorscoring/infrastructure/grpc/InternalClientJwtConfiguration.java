package com.finanscore.motorscoring.infrastructure.grpc;

import com.finanscore.motorscoring.infrastructure.security.jwt.PemKeyLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPublicKey;

@Configuration
@Profile("api")
public class InternalClientJwtConfiguration {
    @Bean("internalClientJwtDecoder")
    JwtDecoder internalClientJwtDecoder(@Value("${iam.jwt.public-key}") String publicKeyPath) {
        RSAPublicKey publicKey = PemKeyLoader.publicKey(publicKeyPath);
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
