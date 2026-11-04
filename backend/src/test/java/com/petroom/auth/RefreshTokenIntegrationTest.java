package com.petroom.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petroom.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RefreshTokenIntegrationTest extends IntegrationTest {

    private String signupRefreshToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\",\"nickname\":\"하랑\"}".formatted(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.refreshToken");
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    @Test
    @DisplayName("리프레시 토큰으로 새 토큰 쌍을 받고, 쓴 토큰은 다시 쓸 수 없다")
    void rotates() throws Exception {
        String first = signupRefreshToken();

        String response = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String second = JsonPath.read(response, "$.refreshToken");

        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // 이미 쓴 토큰이 다시 오면 탈취로 보고, 새로 받은 토큰도 함께 무효화한다
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 리프레시 토큰을 쓸 수 없다")
    void logout() throws Exception {
        String token = signupRefreshToken();

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("모르는 토큰은 401")
    void unknown() throws Exception {
        refresh("not-a-real-token").andExpect(status().isUnauthorized());
    }
}
