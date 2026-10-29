package com.petroom.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petroom.auth.oauth.OAuthClient;
import com.petroom.auth.oauth.OAuthClients;
import com.petroom.auth.oauth.OAuthProfile;
import com.petroom.auth.oauth.OAuthProvider;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

class SocialLoginIntegrationTest extends IntegrationTest {

    private static final String REDIRECT = "http://localhost:3000/oauth/callback";

    @MockitoBean
    private OAuthClients clients;

    private ResultActions login(String provider, String redirect) throws Exception {
        return mockMvc.perform(post("/api/auth/oauth/" + provider).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"auth-code\",\"redirectUri\":\"%s\"}".formatted(redirect)));
    }

    private void givenProfile(OAuthProfile profile) {
        OAuthClient client = (code, redirectUri) -> profile;
        when(clients.get(any())).thenReturn(client);
    }

    @Test
    @DisplayName("처음 들어온 카카오 계정은 가입되고, 다시 들어오면 같은 사용자로 로그인된다")
    void firstLoginRegisters() throws Exception {
        String kakaoId = UUID.randomUUID().toString();
        givenProfile(new OAuthProfile(kakaoId, null, false, "카카오 하랑"));

        String first = login("kakao", REDIRECT).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = login("kakao", REDIRECT).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String me1 = mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(first, "$.accessToken")))
                .andExpect(jsonPath("$.nickname").value("카카오 하랑"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String me2 = mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(second, "$.accessToken")))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((Object) JsonPath.read(me1, "$.id"))
                .isEqualTo(JsonPath.read(me2, "$.id"));
    }

    @Test
    @DisplayName("같은 이메일의 기존 계정과 자동으로 합치지 않는다")
    void doesNotMergeByEmail() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"nickname\":\"기존\"}".formatted(email)))
                .andExpect(status().isCreated());
        givenProfile(new OAuthProfile(UUID.randomUUID().toString(), email, true, "구글 사용자"));

        String response = login("google", REDIRECT).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(response, "$.accessToken")))
                .andExpect(jsonPath("$.nickname").value("구글 사용자"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("허용되지 않은 콜백 주소, 비활성 제공자, 제공자 오류를 구분해 응답한다")
    void failures() throws Exception {
        givenProfile(new OAuthProfile("x", null, false, null));
        login("kakao", "https://evil.example.com/callback")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REDIRECT_URI"));

        when(clients.get(OAuthProvider.GOOGLE)).thenThrow(new BusinessException(ErrorCode.OAUTH_PROVIDER_DISABLED));
        login("google", REDIRECT)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OAUTH_PROVIDER_DISABLED"));

        OAuthClient broken = (code, redirectUri) -> {
            throw new IllegalStateException("invalid_grant");
        };
        when(clients.get(OAuthProvider.KAKAO)).thenReturn(broken);
        login("kakao", REDIRECT)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("OAUTH_FAILED"));
    }
}
