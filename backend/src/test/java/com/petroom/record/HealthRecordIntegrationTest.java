package com.petroom.record;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

class HealthRecordIntegrationTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private String bearer;
    private long dog;
    private long tank;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
        dog = createPet(bearer, DOG_JSON);
        tank = createPet(bearer, TANK_JSON);
    }

    private ResultActions putRecord(long petId, LocalDate date, String body) throws Exception {
        return mockMvc.perform(put("/api/pets/" + petId + "/health-records/" + date)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("같은 날짜에 다시 저장하면 덮어쓴다")
    void putOverwrites() throws Exception {
        putRecord(dog, today, """
                {"weightKg":5.2,"meal":"NORMAL","stool":"NORMAL","memo":"산책 후 컨디션 좋음"}
                """).andExpect(status().isOk());

        putRecord(dog, today, """
                {"meal":"LOW"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meal").value("LOW"))
                .andExpect(jsonPath("$.weightKg").doesNotExist())
                .andExpect(jsonPath("$.memo").doesNotExist());

        mockMvc.perform(get("/api/pets/" + dog + "/health-records").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("오늘 이후 날짜, 빈 기록, 어항에 식사 기록은 거절한다")
    void rejectsInvalid() throws Exception {
        putRecord(dog, today.plusDays(1), "{\"meal\":\"NORMAL\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECORD"));

        putRecord(dog, today, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECORD"));

        putRecord(tank, today, "{\"meal\":\"NORMAL\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECORD"));

        putRecord(dog, today, "{\"fed\":\"DONE\"}")
                .andExpect(status().isBadRequest());

        putRecord(tank, today, "{\"fed\":\"DONE\"}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일괄 기록은 여러 마리를 한 번에 저장하고, 기존 체중·메모는 유지한다")
    void batchMerges() throws Exception {
        putRecord(dog, today, "{\"weightKg\":5.2,\"memo\":\"메모 유지\"}").andExpect(status().isOk());

        mockMvc.perform(post("/api/health-records/batch")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"%s","items":[
                                  {"petId":%d,"meal":"NORMAL","stool":"NORMAL"},
                                  {"petId":%d,"fed":"DONE"}]}
                                """.formatted(today, dog, tank)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weightKg").value(5.2))
                .andExpect(jsonPath("$[0].memo").value("메모 유지"))
                .andExpect(jsonPath("$[0].meal").value("NORMAL"))
                .andExpect(jsonPath("$[1].fed").value("DONE"));
    }

    @Test
    @DisplayName("일괄 기록에 남의 반려동물이 섞이면 아무것도 저장하지 않는다")
    void batchRejectsOthersPet() throws Exception {
        String other = signupAndGetBearer();
        long othersPet = createPet(other, DOG_JSON);

        mockMvc.perform(post("/api/health-records/batch")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"%s","items":[
                                  {"petId":%d,"meal":"NORMAL"},
                                  {"petId":%d,"meal":"NORMAL"}]}
                                """.formatted(today, dog, othersPet)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/pets/" + dog + "/health-records").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("기간 조회와 삭제")
    void listAndDelete() throws Exception {
        putRecord(dog, today.minusDays(40), "{\"meal\":\"NORMAL\"}").andExpect(status().isOk());
        putRecord(dog, today.minusDays(1), "{\"meal\":\"NORMAL\"}").andExpect(status().isOk());

        mockMvc.perform(get("/api/pets/" + dog + "/health-records").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/pets/" + dog + "/health-records")
                        .param("from", today.minusDays(60).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value(today.minusDays(1).toString()));

        mockMvc.perform(delete("/api/pets/" + dog + "/health-records/" + today.minusDays(1))
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/pets/" + dog + "/health-records/" + today.minusDays(1))
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HEALTH_RECORD_NOT_FOUND"));
    }
}
