package com.petroom.special;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petroom.support.IntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SpecialRecordIntegrationTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private String bearer;
    private long bird;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
        bird = createPet(bearer, BIRD_JSON);
    }

    private ResultActions record(long petId, String item, LocalDate date, String value) throws Exception {
        return mockMvc.perform(post("/api/pets/" + petId + "/special-records")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"item\":\"%s\",\"recordDate\":\"%s\",\"value\":\"%s\"}".formatted(item, date, value)));
    }

    @Test
    @DisplayName("조류는 부리·호흡기(사용자 지정)와 제안 항목을 가지며, 최근 기록이 함께 보인다")
    void birdItems() throws Exception {
        record(bird, "BEAK", today.minusDays(9), "길이 정상").andExpect(status().isCreated());
        record(bird, "BEAK", today, "끝 마모").andExpect(status().isCreated());

        mockMvc.perform(get("/api/pets/" + bird + "/special-items").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.item=='BEAK')].userDefined").value(true))
                .andExpect(jsonPath("$[?(@.item=='BEAK')].latest.value").value("끝 마모"))
                .andExpect(jsonPath("$[?(@.item=='MOLT')].userDefined").value(false));
    }

    @Test
    @DisplayName("분류에 없는 항목은 기록할 수 없다")
    void rejectsForeignItem() throws Exception {
        record(bird, "WATER_QUALITY", today, "pH 7.0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SPECIAL_ITEM"));

        record(bird, "UNKNOWN", today, "x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("항목별로 기록을 조회한다")
    void listByItem() throws Exception {
        record(bird, "BEAK", today, "정상").andExpect(status().isCreated());
        record(bird, "NAIL", today, "다듬음").andExpect(status().isCreated());

        mockMvc.perform(get("/api/pets/" + bird + "/special-records").param("item", "NAIL")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("발톱 관리"));
    }
}
