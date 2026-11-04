package com.petroom.push;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petroom.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class PushDisabledIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("VAPID 키가 없으면 웹 푸시는 꺼져 있고 나머지 기능은 그대로 동작한다")
    void disabled() throws Exception {
        String bearer = signupAndGetBearer();
        mockMvc.perform(get("/api/push/public-key").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PUSH_DISABLED"));
    }
}
