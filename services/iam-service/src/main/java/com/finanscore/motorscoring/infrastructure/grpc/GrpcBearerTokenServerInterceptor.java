package com.finanscore.motorscoring.infrastructure.grpc;

import io.grpc.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.naming.ldap.LdapName;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;
import java.util.*;

/** Defense in depth: mTLS autentica el workload y el JWT OAuth2 autoriza el scope. */
@Component
public class GrpcBearerTokenServerInterceptor implements ServerInterceptor {
    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final JwtDecoder decoder;
    private final String issuer;
    private final String audience;
    private final String requiredScope;

    public GrpcBearerTokenServerInterceptor(
            @Qualifier("internalClientJwtDecoder") JwtDecoder decoder,
            @Value("${iam.internal-oauth.issuer:https://localhost:8443}") String issuer,
            @Value("${iam.internal-oauth.audience:iam-grpc}") String audience,
            @Value("${iam.grpc.required-scope:iam.user.read}") String requiredScope) {
        this.decoder = decoder;
        this.issuer = issuer;
        this.audience = audience;
        this.requiredScope = requiredScope;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        try {
            String auth = headers.get(AUTHORIZATION);
            if (auth == null || !auth.startsWith("Bearer ")) {
                return deny(call, "Bearer token requerido");
            }

            SSLSession ssl = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
            if (ssl == null) return deny(call, "mTLS requerido");
            String certificateCn = peerCommonName(ssl);

            Jwt jwt = decoder.decode(auth.substring(7).trim());
            if (!issuer.equals(jwt.getIssuer() == null ? null : jwt.getIssuer().toString())) return deny(call, "issuer inválido");
            if (!jwt.getAudience().contains(audience)) return deny(call, "audience inválido");
            if (!"CLIENT_CREDENTIALS".equals(jwt.getClaimAsString("token_use"))) return deny(call, "tipo de token inválido");
            if (!scopeContains(jwt.getClaimAsString("scope"), requiredScope)) return deny(call, "scope insuficiente");
            if (!Objects.equals(jwt.getSubject(), certificateCn)) return deny(call, "identidad mTLS/JWT no coincide");

            return next.startCall(call, headers);
        } catch (JwtException | SSLPeerUnverifiedException ex) {
            return deny(call, "credenciales internas inválidas");
        } catch (Exception ex) {
            return deny(call, "autenticación interna fallida");
        }
    }

    private static boolean scopeContains(String scope, String required) {
        return scope != null && Arrays.asList(scope.split("\\s+")).contains(required);
    }

    private static String peerCommonName(SSLSession session) throws Exception {
        var certificates = session.getPeerCertificates();
        if (certificates.length == 0 || !(certificates[0] instanceof X509Certificate certificate)) {
            throw new SSLPeerUnverifiedException("Certificado cliente ausente");
        }
        LdapName name = new LdapName(certificate.getSubjectX500Principal().getName());
        return name.getRdns().stream()
                .filter(rdn -> "CN".equalsIgnoreCase(rdn.getType()))
                .map(rdn -> String.valueOf(rdn.getValue()))
                .findFirst()
                .orElseThrow(() -> new SSLPeerUnverifiedException("CN cliente ausente"));
    }

    private static <ReqT> ServerCall.Listener<ReqT> deny(ServerCall<ReqT, ?> call, String reason) {
        call.close(Status.UNAUTHENTICATED.withDescription(reason), new Metadata());
        return new ServerCall.Listener<>() {};
    }
}
