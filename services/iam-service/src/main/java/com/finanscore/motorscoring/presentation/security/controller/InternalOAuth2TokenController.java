package com.finanscore.motorscoring.presentation.security.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/**
 * Endpoint OAuth2 Client Credentials interno para llamadas service-to-service.
 * En V3.6 se usa por Notification Service para obtener un JWT corto antes de
 * invocar UserDirectory por gRPC+mTLS.
 */
@RestController
@RequestMapping("/oauth2")
public class InternalOAuth2TokenController {
    private static final String CLIENT_ID = "notification-service";
    private static final String ALLOWED_SCOPE = "iam.user.read";

    private final JwtEncoder encoder;
    private final String clientSecret;
    private final String issuer;
    private final String audience;
    private final Duration ttl;

    public InternalOAuth2TokenController(
            JwtEncoder encoder,
            @Value("${iam.internal-oauth.notification-client-secret}") String clientSecret,
            @Value("${iam.internal-oauth.issuer:https://localhost:8443}") String issuer,
            @Value("${iam.internal-oauth.audience:iam-grpc}") String audience,
            @Value("${iam.internal-oauth.access-seconds:300}") long accessSeconds) {
        this.encoder = encoder;
        this.clientSecret = Objects.requireNonNull(clientSecret, "OAuth2 client secret requerido");
        this.issuer = issuer;
        this.audience = audience;
        this.ttl = Duration.ofSeconds(accessSeconds);
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "scope", required = false) String requestedScope) {

        if (!"client_credentials".equals(grantType)) {
            return oauthError(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "Solo client_credentials está habilitado.");
        }

        ClientCredentials credentials = parseBasic(authorization);
        if (credentials == null || !CLIENT_ID.equals(credentials.clientId()) ||
                !constantTimeEquals(clientSecret, credentials.clientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"finanscore-internal-oauth2\"")
                    .body(Map.of("error", "invalid_client"));
        }

        String scope = requestedScope == null || requestedScope.isBlank() ? ALLOWED_SCOPE : requestedScope.trim();
        if (!ALLOWED_SCOPE.equals(scope)) {
            return oauthError(HttpStatus.BAD_REQUEST, "invalid_scope", "Scope no permitido.");
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(CLIENT_ID)
                .audience(List.of(audience))
                .issuedAt(now)
                .notBefore(now.minusSeconds(5))
                .expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString())
                .claim("client_id", CLIENT_ID)
                .claim("scope", scope)
                .claim("token_use", "CLIENT_CREDENTIALS")
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(Map.of(
                        "access_token", accessToken,
                        "token_type", "Bearer",
                        "expires_in", ttl.toSeconds(),
                        "scope", scope));
    }

    private static ResponseEntity<Map<String, String>> oauthError(HttpStatus status, String code, String description) {
        return ResponseEntity.status(status).body(Map.of("error", code, "error_description", description));
    }

    private static ClientCredentials parseBasic(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator <= 0) return null;
            return new ClientCredentials(decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        if (expected == null || supplied == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    private record ClientCredentials(String clientId, String clientSecret) {}
}
