package com.petroom.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** 구글 로그인 (OpenID Connect): https://developers.google.com/identity/openid-connect/openid-connect */
public class GoogleOAuthClient implements OAuthClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final RestClient restClient;
    private final OAuthProperties.Client client;

    public GoogleOAuthClient(RestClient restClient, OAuthProperties.Client client) {
        this.restClient = restClient;
        this.client = client;
    }

    @Override
    public OAuthProfile fetchProfile(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", client.clientId());
        form.add("client_secret", client.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        JsonNode token = restClient.post().uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        String accessToken = token == null ? null : token.path("access_token").asText(null);
        if (accessToken == null) {
            throw new IllegalStateException("Google token response has no access_token");
        }

        JsonNode user = restClient.get().uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);
        if (user == null || !user.hasNonNull("sub")) {
            throw new IllegalStateException("Google userinfo has no sub");
        }
        return new OAuthProfile(user.get("sub").asText(), user.path("email").asText(null),
                user.path("email_verified").asBoolean(false), user.path("name").asText(null));
    }
}
