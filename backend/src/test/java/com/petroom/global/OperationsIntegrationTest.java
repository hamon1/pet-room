package com.petroom.global;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petroom.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Import(OperationsIntegrationTest.BoomController.class)
class OperationsIntegrationTest extends IntegrationTest {

    /** 테스트에서만 등록되는, 예상하지 못한 오류를 내는 엔드포인트 */
    @RestController
    static class BoomController {
        @GetMapping("/api/test/boom")
        String boom() {
            throw new IllegalStateException("db password=secret");
        }
    }

    @Test
    @DisplayName("모든 응답에 요청 ID를 붙이고, 안전한 형식의 요청 ID는 이어서 쓴다")
    void requestId() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[a-f0-9]{16}")));
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "abc-12345678"))
                .andExpect(header().string("X-Request-Id", "abc-12345678"));
        mockMvc.perform(get("/actuator/health").header("X-Request-Id", "<script>"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[a-f0-9]{16}")));
    }

    @Test
    @DisplayName("예상하지 못한 오류는 내부 메시지 없이 500과 요청 ID를 돌려준다")
    void unexpectedError() throws Exception {
        String bearer = signupAndGetBearer();
        mockMvc.perform(get("/api/test/boom").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    @DisplayName("없는 경로는 500이 아니라 404")
    void notFoundStays404() throws Exception {
        String bearer = signupAndGetBearer();
        mockMvc.perform(get("/api/no-such-path").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
    }
}
