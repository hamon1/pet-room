package com.petroom.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petroom.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SettingsIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("설정을 바꾼 적이 없으면 기본값, 저장 후에는 저장한 값을 돌려준다")
    void defaultsAndUpdate() throws Exception {
        String bearer = signupAndGetBearer();
        mockMvc.perform(get("/api/users/me/settings").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicalNoticeDays").value(1))
                .andExpect(jsonPath("$.treatmentNoticeDays").value(0))
                .andExpect(jsonPath("$.expiryNoticeDays").value(3))
                .andExpect(jsonPath("$.rebuyNoticeDays").value(5))
                .andExpect(jsonPath("$.theme").value("PASTEL"));

        String body = """
                {"medicalNoticeDays":2,"treatmentNoticeDays":1,"expiryNoticeDays":7,"rebuyNoticeDays":10,"theme":"MONOCHROME"}
                """;
        for (int i = 0; i < 2; i++) { // 두 번째 저장은 갱신
            mockMvc.perform(put("/api/users/me/settings").header(HttpHeaders.AUTHORIZATION, bearer)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/users/me/settings").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.expiryNoticeDays").value(7))
                .andExpect(jsonPath("$.theme").value("MONOCHROME"));
    }

    @Test
    @DisplayName("알림 시점은 0~30일만 허용한다")
    void rejectsOutOfRange() throws Exception {
        String bearer = signupAndGetBearer();
        mockMvc.perform(put("/api/users/me/settings").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicalNoticeDays":31,"treatmentNoticeDays":0,"expiryNoticeDays":3,"rebuyNoticeDays":5,"theme":"PASTEL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
