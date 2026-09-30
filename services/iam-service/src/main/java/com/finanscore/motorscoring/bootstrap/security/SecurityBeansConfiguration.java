package com.finanscore.motorscoring.bootstrap.security;

import com.finanscore.motorscoring.application.security.port.in.*;
import com.finanscore.motorscoring.application.security.port.out.*;
import com.finanscore.motorscoring.application.security.service.*;
import com.finanscore.motorscoring.infrastructure.security.crypto.*;
import com.finanscore.motorscoring.infrastructure.security.email.SmtpEmailSenderAdapter;
import com.finanscore.motorscoring.infrastructure.security.jwt.*;
import com.finanscore.motorscoring.infrastructure.security.oauth.TikTokOAuthAdapter;
import com.finanscore.motorscoring.infrastructure.security.totp.TotpAdapter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.client.RestClient;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Profile("api")
public class SecurityBeansConfiguration {
    @Bean PasswordHasherPort passwordHasher(){ return new Argon2PasswordHasherAdapter(); }

    @Bean VerificationCodePort verificationCodes(@Value("${iam.verification.pepper}") String pepper){
        return new HmacVerificationCodeAdapter(resolveSecret(pepper));
    }

    @Bean SecretCipherPort secretCipher(@Value("${iam.totp.aes-key-base64}") String key){
        return new AesGcmSecretCipherAdapter(resolveSecret(key));
    }

    @Bean TotpPort totp(Clock clock){ return new TotpAdapter(clock); }

    @Bean EmailSenderPort emailSender(JavaMailSender sender,@Value("${iam.mail.from}") String from){
        return new SmtpEmailSenderAdapter(sender,from);
    }

    @Bean RSAPrivateKey iamPrivateKey(@Value("${iam.jwt.private-key}") String path){
        return PemKeyLoader.privateKey(path);
    }

    @Bean RSAPublicKey iamPublicKey(@Value("${iam.jwt.public-key}") String path){
        return PemKeyLoader.publicKey(path);
    }

    @Bean JwtEncoder jwtEncoder(RSAPublicKey pub, RSAPrivateKey priv){
        RSAKey rsa = new RSAKey.Builder(pub)
            .privateKey(priv)
            .keyID("finanscore-rsa-1")
            .build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsa));
        return new NimbusJwtEncoder(source);
    }

    /*
     * El Resource Server valida exclusivamente Access JWT de la API.
     * Los tokens temporales MFA se verifican dentro de JwtTokenAdapter con la
     * misma clave pública, pero con reglas separadas de los Access JWT.
     */
    @Bean("resourceServerJwtDecoder")
    JwtDecoder resourceServerJwtDecoder(RSAPublicKey pub){
        return NimbusJwtDecoder.withPublicKey(pub).build();
    }

    @Bean
    TokenPort tokenPort(
        JwtEncoder encoder,
        RSAPublicKey pub,
        @Value("${iam.jwt.issuer}") String issuer,
        @Value("${iam.jwt.audience}") String audience) {
        return new JwtTokenAdapter(encoder, pub, issuer, audience);
    }

    @Bean
    ExternalIdentityProviderPort externalProvider(
        RestClient.Builder builder,
        @Value("${iam.oauth.tiktok.client-key}") String key,
        @Value("${iam.oauth.tiktok.client-secret}") String secret) {
        return new TikTokOAuthAdapter(builder,key,secret);
    }

    @Bean
    RegisterUserUseCase register(
        UserAccountRepositoryPort users,
        LocalCredentialRepositoryPort credentials,
        EmailVerificationRepositoryPort verifications,
        AuthorizationRepositoryPort authorization,
        MfaTotpRepositoryPort mfa,
        PasswordHasherPort passwordHasher,
        VerificationCodePort codes,
        EmailSenderPort mail,
        TokenPort tokens,
        Clock clock) {
        return new RegisterUserService(
            users, credentials, verifications, authorization, mfa,
            passwordHasher, codes, mail, tokens, clock);
    }

    @Bean
    VerifyEmailUseCase verifyEmail(
        UserAccountRepositoryPort users,
        EmailVerificationRepositoryPort verifications,
        VerificationCodePort codes,
        MfaTotpRepositoryPort mfa,
        TokenPort tokens,
        Clock clock) {
        return new VerifyEmailService(users,verifications,codes,mfa,tokens,clock);
    }

    @Bean
    LoginLocalUseCase login(
        UserAccountRepositoryPort users,
        LocalCredentialRepositoryPort credentials,
        MfaTotpRepositoryPort mfa,
        PasswordHasherPort passwordHasher,
        TokenPort tokens,
        Clock clock) {
        return new LoginLocalService(users,credentials,mfa,passwordHasher,tokens,clock);
    }

    @Bean
    SetupMfaUseCase setup(
        TokenPort tokens,
        UserAccountRepositoryPort users,
        MfaTotpRepositoryPort mfa,
        TotpPort totp,
        SecretCipherPort cipher,
        Clock clock,
        @Value("${iam.totp.issuer:FinanScore}") String issuer) {
        return new SetupMfaService(tokens,users,mfa,totp,cipher,clock,issuer);
    }

    @Bean
    ConfirmMfaUseCase confirm(
        TokenPort tokens,
        MfaTotpRepositoryPort mfa,
        TotpPort totp,
        SecretCipherPort cipher,
        AuthorizationRepositoryPort authorization,
        SessionRepositoryPort sessions,
        UserAccountRepositoryPort users,
        Clock clock,
        @Value("${iam.jwt.access-minutes:10}") long accessMin,
        @Value("${iam.jwt.refresh-days:7}") long refreshDays) {
        return new ConfirmMfaService(
            tokens,mfa,totp,cipher,authorization,sessions,users,clock,
            Duration.ofMinutes(accessMin),Duration.ofDays(refreshDays));
    }

    @Bean
    VerifyMfaUseCase verifyMfa(
        TokenPort tokens,
        MfaTotpRepositoryPort mfa,
        SecretCipherPort cipher,
        TotpPort totp,
        AuthorizationRepositoryPort authorization,
        SessionRepositoryPort sessions,
        UserAccountRepositoryPort users,
        Clock clock,
        @Value("${iam.jwt.access-minutes:10}") long accessMin,
        @Value("${iam.jwt.refresh-days:7}") long refreshDays) {
        return new VerifyMfaService(
            tokens,mfa,cipher,totp,authorization,sessions,users,clock,
            Duration.ofMinutes(accessMin),Duration.ofDays(refreshDays));
    }

    @Bean
    RefreshSessionUseCase refresh(
        TokenPort tokens,
        SessionRepositoryPort sessions,
        AuthorizationRepositoryPort authorization,
        Clock clock,
        @Value("${iam.jwt.access-minutes:10}") long accessMin,
        @Value("${iam.jwt.refresh-days:7}") long refreshDays) {
        return new RefreshSessionService(
            tokens,sessions,authorization,clock,
            Duration.ofMinutes(accessMin),Duration.ofDays(refreshDays));
    }

    @Bean
    LogoutUseCase logout(TokenPort tokens,SessionRepositoryPort sessions,Clock clock){
        return new LogoutService(tokens,sessions,clock);
    }

    @Bean
    GetCurrentUserUseCase currentUser(UserAccountRepositoryPort users) {
        return new GetCurrentUserService(users);
    }

    @Bean
    ProcessSocialIdentityUseCase processSocial(
        SocialIdentityRepositoryPort identities,
        UserAccountRepositoryPort users,
        AuthorizationRepositoryPort authorization,
        MfaTotpRepositoryPort mfa,
        TokenPort tokens,
        Clock clock) {
        return new ProcessSocialIdentityService(
            identities,users,authorization,mfa,tokens,clock);
    }

    @Bean
    LoginSocialUseCase loginSocial(
        ExternalIdentityProviderPort provider,
        ProcessSocialIdentityUseCase process) {
        return new LoginSocialService(provider,process);
    }
    /** Permite usar secretos directos por variable de entorno o file:/ruta en Docker. */
    private static String resolveSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Secreto IAM no configurado.");
        }
        if (!value.startsWith("file:")) {
            return value.trim();
        }
        try {
            return Files.readString(Path.of(value.substring("file:".length()))).trim();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer secreto IAM desde " + value, e);
        }
    }

}
