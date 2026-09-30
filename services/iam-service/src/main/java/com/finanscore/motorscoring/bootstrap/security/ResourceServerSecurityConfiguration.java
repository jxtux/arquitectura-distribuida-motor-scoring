package com.finanscore.motorscoring.bootstrap.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import java.util.*;

@Configuration
@Profile("api")
@EnableMethodSecurity
public class ResourceServerSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        @Qualifier("resourceServerJwtDecoder") JwtDecoder decoder,
        GoogleOidcSuccessHandler googleHandler,
        @Value("${iam.jwt.issuer}") String issuer,
        @Value("${iam.jwt.audience}") String audience) throws Exception {

        if (decoder instanceof NimbusJwtDecoder nimbus) {
            OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuer);

            OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token","Audience inválido",null));

            OAuth2TokenValidator<Jwt> accessOnly = jwt ->
                "ACCESS".equals(jwt.getClaimAsString("token_use"))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token","No es access token",null));

            OAuth2TokenValidator<Jwt> mfaRequired = jwt ->
                Boolean.TRUE.equals(jwt.getClaim("mfa"))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token","MFA requerido",null));

            nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, audienceValidator, accessOnly, mfaRequired));
        }

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<org.springframework.security.core.GrantedAuthority> out = new HashSet<>();
            List<String> permissions = jwt.getClaimAsStringList("permissions");
            List<String> roles = jwt.getClaimAsStringList("roles");

            if (permissions != null) {
                permissions.forEach(p ->
                    out.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(p)));
            }
            if (roles != null) {
                roles.forEach(r ->
                    out.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r)));
            }
            return out;
        });

        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/actuator/health/**",
                    "/actuator/prometheus",
                    "/actuator/info",
                    "/terms",
                    "/privacy"
                ).permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth -> oauth.successHandler(googleHandler))
            .oauth2ResourceServer(rs ->
                rs.jwt(jwt ->
                    jwt.decoder(decoder)
                       .jwtAuthenticationConverter(converter)))
            .build();
    }
}
