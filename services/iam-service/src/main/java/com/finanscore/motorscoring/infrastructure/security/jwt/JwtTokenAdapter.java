package com.finanscore.motorscoring.infrastructure.security.jwt;

import com.finanscore.motorscoring.application.security.model.IssuedSession;
import com.finanscore.motorscoring.application.security.port.out.TokenPort;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.util.*;
import java.util.HexFormat;

/**
 * Emite los JWT internos de Motor Scoring.
 *
 * FIXED13:
 * Los tokens temporales MFA se verifican de forma aislada del JwtDecoder del
 * Resource Server. Se valida explícitamente firma RS256, expiración, nbf,
 * issuer, audience, token_use y purpose.
 *
 * De esta forma un token recién emitido para MFA_SETUP no puede ser rechazado
 * por validadores pensados exclusivamente para Access JWT (mfa=true,
 * token_use=ACCESS, etc.).
 */
public final class JwtTokenAdapter implements TokenPort {
    private static final long CLOCK_SKEW_SECONDS = 30L;

    private final JwtEncoder encoder;
    private final RSAPublicKey verificationKey;
    private final String issuer;
    private final String audience;
    private final SecureRandom random = new SecureRandom();

    public JwtTokenAdapter(
        JwtEncoder encoder,
        RSAPublicKey verificationKey,
        String issuer,
        String audience) {
        this.encoder = encoder;
        this.verificationKey = verificationKey;
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public String issueTemporaryToken(Long userId, String purpose, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(userId.toString())
            .issuedAt(now)
            .notBefore(now.minusSeconds(CLOCK_SKEW_SECONDS))
            .expiresAt(now.plus(ttl))
            .id(UUID.randomUUID().toString())
            .claim("token_use", "TEMP")
            .claim("purpose", purpose)
            .build();
        return encode(claims);
    }

    @Override
    public Long validateTemporaryToken(String token, String expectedPurpose) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token temporal vacío.");
        }

        try {
            SignedJWT signed = SignedJWT.parse(token.trim());

            if (!JWSAlgorithm.RS256.equals(signed.getHeader().getAlgorithm())) {
                throw new IllegalArgumentException("Algoritmo de token temporal inválido.");
            }

            if (!signed.verify(new RSASSAVerifier(verificationKey))) {
                throw new IllegalArgumentException("Firma de token temporal inválida.");
            }

            JWTClaimsSet claims = signed.getJWTClaimsSet();
            Instant now = Instant.now();

            Date expiresAt = claims.getExpirationTime();
            if (expiresAt == null || now.isAfter(expiresAt.toInstant().plusSeconds(CLOCK_SKEW_SECONDS))) {
                throw new IllegalArgumentException("Token temporal expirado.");
            }

            Date notBefore = claims.getNotBeforeTime();
            if (notBefore != null && now.plusSeconds(CLOCK_SKEW_SECONDS).isBefore(notBefore.toInstant())) {
                throw new IllegalArgumentException("Token temporal todavía no válido.");
            }

            if (!issuer.equals(claims.getIssuer())) {
                throw new IllegalArgumentException("Issuer de token temporal inválido.");
            }

            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(audience)) {
                throw new IllegalArgumentException("Audience de token temporal inválido.");
            }

            if (!"TEMP".equals(claims.getStringClaim("token_use"))) {
                throw new IllegalArgumentException("Uso de token temporal inválido.");
            }

            if (!expectedPurpose.equals(claims.getStringClaim("purpose"))) {
                throw new IllegalArgumentException("Propósito de token temporal inválido.");
            }

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("Subject de token temporal inválido.");
            }

            return Long.valueOf(subject);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Token temporal inválido.", ex);
        }
    }

    @Override
    public IssuedSession issueSession(
        Long userId,
        Set<String> roles,
        Set<String> permissions,
        Duration accessTtl,
        Duration refreshTtl) {

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(audience))
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(accessTtl))
            .id(UUID.randomUUID().toString())
            .claim("token_use", "ACCESS")
            .claim("mfa", true)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .build();

        byte[] refresh = new byte[32];
        random.nextBytes(refresh);
        String rawRefresh =
            Base64.getUrlEncoder().withoutPadding().encodeToString(refresh);

        return new IssuedSession(
            encode(claims),
            accessTtl.toSeconds(),
            rawRefresh,
            refreshTtl.toSeconds());
    }

    @Override
    public String hashRefreshToken(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header =
            JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return encoder.encode(
            JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
