package com.finanscore.motorscoring.presentation.security.controller;

import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.in.LoginSocialUseCase;
import org.springframework.context.annotation.Profile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@RestController
@Profile("api")
@RequestMapping("/api/v1/auth/social/tiktok")
public class TikTokOAuthController {
    private static final String STATE_COOKIE = "fs_tiktok_state";

    private final LoginSocialUseCase social;
    private final String clientKey;
    private final String callback;
    private final String frontendCallback;
    private final SecureRandom random = new SecureRandom();

    public TikTokOAuthController(
        LoginSocialUseCase social,
        @Value("${iam.oauth.tiktok.client-key}") String clientKey,
        @Value("${iam.oauth.tiktok.redirect-uri}") String callback,
        @Value("${iam.frontend.social-callback}") String frontendCallback) {
        this.social = social;
        this.clientKey = clientKey;
        this.callback = callback;
        this.frontendCallback = frontendCallback;
    }

    /**
     * Angular corre en localhost pero TikTok puede usar un callback público ngrok.
     * El frontend descubre el host correcto desde TIKTOK_REDIRECT_URI para iniciar
     * OAuth en el MISMO host que recibirá callback/state.
     */
    @GetMapping("/start-url")
    public Map<String,String> startUrl() {
        URI uri = URI.create(callback);
        String origin = uri.getScheme() + "://" + uri.getRawAuthority();
        return Map.of("url", origin + "/api/v1/auth/social/tiktok/start");
    }

    @GetMapping("/start")
    public void start(HttpServletResponse response) throws IOException {
        byte[] stateBytes = new byte[24];
        random.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, state)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api/v1/auth/social/tiktok")
            .maxAge(300)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String url =
            "https://www.tiktok.com/v2/auth/authorize/"
            + "?client_key=" + enc(clientKey)
            + "&response_type=code"
            + "&scope=user.info.basic"
            + "&redirect_uri=" + enc(callback)
            + "&state=" + enc(state)
            // TikTok Web documenta disable_auto_auth=1 para mostrar siempre
            // la pantalla de autorización. En Sandbox facilita seleccionar
            // explícitamente una cuenta incluida en Target Users.
            + "&disable_auto_auth=1";

        response.sendRedirect(url);
    }

    @GetMapping("/callback")
    public void callback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        @RequestParam(name = "error_description", required = false) String errorDescription,
        @CookieValue(name = STATE_COOKIE, required = false) String expectedState,
        HttpServletResponse response) throws IOException {

        if (expectedState == null || state == null
            || !java.security.MessageDigest.isEqual(
                state.getBytes(StandardCharsets.UTF_8),
                expectedState.getBytes(StandardCharsets.UTF_8))) {
            clearStateCookie(response);
            redirectError(response, "TIKTOK_STATE_INVALID", "La sesión OAuth de TikTok no es válida o expiró.");
            return;
        }

        clearStateCookie(response);

        if (error != null && !error.isBlank()) {
            String message = errorDescription == null || errorDescription.isBlank()
                ? "TikTok rechazó o canceló la autorización."
                : errorDescription;
            redirectError(response, error, message);
            return;
        }

        if (code == null || code.isBlank()) {
            redirectError(response, "TIKTOK_CODE_MISSING", "TikTok no devolvió el código de autorización.");
            return;
        }

        AuthStage stage = social.login(SocialProvider.TIKTOK, code, callback);

        String target =
            frontendCallback
            + "#token=" + enc(stage.token())
            + "&next=" + enc(stage.nextStep());

        response.sendRedirect(target);
    }

    private void clearStateCookie(HttpServletResponse response) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            ResponseCookie.from(STATE_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/v1/auth/social/tiktok")
                .maxAge(0)
                .build()
                .toString());
    }

    private void redirectError(HttpServletResponse response, String error, String message) throws IOException {
        response.sendRedirect(
            frontendCallback
            + "#error=" + enc(error)
            + "&message=" + enc(message));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
