package com.finanscore.motorscoring.infrastructure.security.oauth;

import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.*;
import com.finanscore.motorscoring.application.security.port.out.ExternalIdentityProviderPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public final class TikTokOAuthAdapter implements ExternalIdentityProviderPort {
    private final RestClient rest;
    private final String clientKey;
    private final String clientSecret;

    public TikTokOAuthAdapter(RestClient.Builder builder, String clientKey, String clientSecret) {
        this.rest = builder.build();
        this.clientKey = clientKey;
        this.clientSecret = clientSecret;
    }

    @Override
    public ExternalIdentity exchangeAuthorizationCode(SocialProvider provider, String code, String redirectUri) {
        if (provider != SocialProvider.TIKTOK) throw new IllegalArgumentException("Adapter solo para TikTok.");

        String body = "client_key=" + enc(clientKey)
            + "&client_secret=" + enc(clientSecret)
            + "&code=" + enc(code)
            + "&grant_type=authorization_code"
            + "&redirect_uri=" + enc(redirectUri);

        TokenResponse token = rest.post()
            .uri("https://open.tiktokapis.com/v2/oauth/token/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve().body(TokenResponse.class);

        if (token == null || token.access_token() == null)
            throw new SecurityApplicationException("TIKTOK_TOKEN_ERROR", "TikTok no devolvió access token.");

        UserEnvelope env = rest.get()
            .uri("https://open.tiktokapis.com/v2/user/info/?fields=open_id,display_name")
            .header("Authorization", "Bearer " + token.access_token())
            .retrieve().body(UserEnvelope.class);

        if (env == null || env.data() == null || env.data().user() == null)
            throw new SecurityApplicationException("TIKTOK_USER_ERROR", "No se pudo obtener identidad TikTok.");

        TikTokUser u = env.data().user();
        return new ExternalIdentity(SocialProvider.TIKTOK, u.open_id(), null, false, u.display_name());
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    record TokenResponse(String access_token, String refresh_token, String open_id, long expires_in) {}
    record UserEnvelope(UserData data) {}
    record UserData(TikTokUser user) {}
    record TikTokUser(String open_id, String display_name) {}
}
