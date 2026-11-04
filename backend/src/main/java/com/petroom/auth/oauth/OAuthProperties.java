package com.petroom.auth.oauth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
public record OAuthProperties(List<String> allowedRedirectUris, Client kakao, Client google) {

    public record Client(String clientId, String clientSecret) {

        public boolean enabled() {
            return clientId != null && !clientId.isBlank();
        }

        public boolean hasSecret() {
            return clientSecret != null && !clientSecret.isBlank();
        }
    }
}
