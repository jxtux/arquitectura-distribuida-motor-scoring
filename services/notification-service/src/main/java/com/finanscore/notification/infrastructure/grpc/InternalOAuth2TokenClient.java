package com.finanscore.notification.infrastructure.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.*;
import java.util.Base64;

/** OAuth2 Client Credentials client with CA pinning for the local Kong HTTPS endpoint. */
@Component
public class InternalOAuth2TokenClient {
    private final HttpClient http;
    private final ObjectMapper json;
    private final URI tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private volatile CachedToken cached;

    public InternalOAuth2TokenClient(
            ObjectMapper json,
            @Value("${app.iam.oauth2.token-uri:https://kong:8443/oauth2/token}") URI tokenUri,
            @Value("${app.iam.oauth2.client-id:notification-service}") String clientId,
            @Value("${app.iam.oauth2.client-secret}") String clientSecret,
            @Value("${app.iam.oauth2.scope:iam.user.read}") String scope,
            @Value("${app.iam.mtls.ca-cert:/run/secrets/ca.crt}") String caCert,
            @Value("${app.iam.connect-timeout-ms:2000}") int connectTimeoutMs) {
        this.json = json;
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.http = HttpClient.newBuilder()
                .sslContext(trustCa(caCert))
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    public String accessToken() {
        CachedToken current = cached;
        Instant now = Instant.now();
        if (current != null && now.isBefore(current.expiresAt().minusSeconds(30))) return current.value();
        synchronized (this) {
            current = cached;
            now = Instant.now();
            if (current != null && now.isBefore(current.expiresAt().minusSeconds(30))) return current.value();
            cached = requestToken(now);
            return cached.value();
        }
    }

    private CachedToken requestToken(Instant now) {
        try {
            String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            String form = "grant_type=client_credentials&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(tokenUri)
                    .header("Authorization", "Basic " + basic)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("OAuth2 token endpoint respondió HTTP " + response.statusCode());
            }
            var body = json.readTree(response.body());
            String token = body.path("access_token").asText();
            long expiresIn = body.path("expires_in").asLong(300);
            if (token.isBlank()) throw new IllegalStateException("OAuth2 access_token ausente");
            return new CachedToken(token, now.plusSeconds(expiresIn));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solicitud OAuth2 interrumpida", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo obtener OAuth2 Client Credentials token", ex);
        }
    }

    private static SSLContext trustCa(String caPath) {
        try (InputStream in = Files.newInputStream(Path.of(caPath))) {
            var ca = CertificateFactory.getInstance("X.509").generateCertificate(in);
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("finanscore-dev-ca", ca);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, tmf.getTrustManagers(), null);
            return ssl;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar CA interna", ex);
        }
    }

    private record CachedToken(String value, Instant expiresAt) {}
}
