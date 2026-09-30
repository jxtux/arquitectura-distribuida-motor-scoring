package com.finanscore.motorscoring.bootstrap.security;

import com.finanscore.motorscoring.application.security.command.ProcessSocialIdentityCommand;
import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.ProcessSocialIdentityUseCase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class GoogleOidcSuccessHandler
    implements AuthenticationSuccessHandler {

    private final ProcessSocialIdentityUseCase process;
    private final String frontendCallback;

    public GoogleOidcSuccessHandler(
        ProcessSocialIdentityUseCase process,
        String frontendCallback) {
        this.process = process;
        this.frontendCallback = frontendCallback;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication)
        throws IOException, ServletException {

        OidcUser oidc = (OidcUser) authentication.getPrincipal();

        ExternalIdentity external = new ExternalIdentity(
            SocialProvider.GOOGLE,
            oidc.getSubject(),          // Identificador estable de Google
            oidc.getEmail(),
            Boolean.TRUE.equals(oidc.getEmailVerified()),
            oidc.getFullName() != null
                ? oidc.getFullName()
                : oidc.getEmail()
        );

        try {
            AuthStage stage = process.process(
                new ProcessSocialIdentityCommand(external));

            // Fragment en vez de query string para reducir exposición en logs.
            response.sendRedirect(
                frontendCallback
                + "#token=" + enc(stage.token())
                + "&next=" + enc(stage.nextStep()));
        } catch (SecurityApplicationException ex) {
            // AuthenticationSuccessHandler vive en la cadena de filtros, fuera del alcance
            // normal de @RestControllerAdvice. Redirigimos un error controlado al frontend
            // en vez de terminar en la Whitelabel Error Page.
            response.sendRedirect(
                frontendCallback
                + "#error=" + enc(ex.code())
                + "&message=" + enc(ex.getMessage()));
        } catch (RuntimeException ex) {
            response.sendRedirect(
                frontendCallback
                + "#error=SOCIAL_LOGIN_FAILED"
                + "&message=" + enc("No se pudo completar el inicio de sesión con Google."));
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
