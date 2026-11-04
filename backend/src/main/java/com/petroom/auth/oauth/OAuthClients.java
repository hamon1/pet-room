package com.petroom.auth.oauth;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 설정에 키가 있는 제공자만 활성화합니다. */
@Component
public class OAuthClients {

    private final Map<OAuthProvider, OAuthClient> clients = new EnumMap<>(OAuthProvider.class);

    public OAuthClients(OAuthProperties properties, RestClient.Builder builder) {
        RestClient restClient = builder.build();
        if (properties.kakao() != null && properties.kakao().enabled()) {
            clients.put(OAuthProvider.KAKAO, new KakaoOAuthClient(restClient, properties.kakao()));
        }
        if (properties.google() != null && properties.google().enabled()) {
            clients.put(OAuthProvider.GOOGLE, new GoogleOAuthClient(restClient, properties.google()));
        }
    }

    public OAuthClient get(OAuthProvider provider) {
        OAuthClient client = provider == null ? null : clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_PROVIDER_DISABLED);
        }
        return client;
    }
}
