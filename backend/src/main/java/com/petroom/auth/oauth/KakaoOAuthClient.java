package com.petroom.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** 카카오 로그인: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api */
public class KakaoOAuthClient implements OAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final OAuthProperties.Client client;

    public KakaoOAuthClient(RestClient restClient, OAuthProperties.Client client) {
        this.restClient = restClient;
        this.client = client;
    }

    @Override
    public OAuthProfile fetchProfile(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", client.clientId());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (client.hasSecret()) {
            form.add("client_secret", client.clientSecret());
        }
        JsonNode token = restClient.post().uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        String accessToken = token == null ? null : token.path("access_token").asText(null);
        if (accessToken == null) {
            throw new IllegalStateException("Kakao token response has no access_token");
        }

        JsonNode user = restClient.get().uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);
        if (user == null || !user.hasNonNull("id")) {
            throw new IllegalStateException("Kakao user response has no id");
        }
        JsonNode account = user.path("kakao_account");
        String email = account.path("email").asText(null);
        boolean verified = account.path("is_email_verified").asBoolean(false)
                && account.path("is_email_valid").asBoolean(false);
        String nickname = account.path("profile").path("nickname").asText(null);
        return new OAuthProfile(user.get("id").asText(), email, verified, nickname);
    }
}
