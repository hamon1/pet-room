package com.petroom.medical;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petroom.support.IntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class MedicalRecordIntegrationTest extends IntegrationTest {

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private String bearer;
    private long dog;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
        dog = createPet(bearer, DOG_JSON);
    }

    private ResultActions create(String auth, long petId, String body) throws Exception {
        return mockMvc.perform(post("/api/pets/" + petId + "/medical-records")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String vaccine(LocalDate date, LocalDate next) {
        return """
                {"kind":"VACCINATION","recordDate":"%s","hospital":" 예시동물병원 ","content":"종합백신 2차","nextDate":%s}
                """.formatted(date, next == null ? "null" : "\"" + next + "\"");
    }

    @Test
    @DisplayName("의료 기록을 등록·수정·삭제한다")
    void crud() throws Exception {
        String response = create(bearer, dog, vaccine(today.minusDays(3), today.plusDays(27)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospital").value("예시동물병원"))
                .andExpect(jsonPath("$.nextDate").value(today.plusDays(27).toString()))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(put("/api/pets/" + dog + "/medical-records/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vaccine(today.minusDays(3), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextDate").doesNotExist());

        mockMvc.perform(get("/api/pets/" + dog + "/medical-records").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/pets/" + dog + "/medical-records/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("기록 날짜가 미래이거나 다음 예정일이 기록 날짜 이전이면 400")
    void dateRules() throws Exception {
        create(bearer, dog, vaccine(today.plusDays(1), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECORD"));

        create(bearer, dog, vaccine(today, today))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECORD"));
    }

    @Test
    @DisplayName("다른 반려동물 경로로 기록을 고치려 하면 404")
    void recordBelongsToPet() throws Exception {
        String response = create(bearer, dog, vaccine(today, null)).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.id")).longValue();
        long cat = createPet(bearer, """
                {"name":"나비","category":"MAMMAL","subCategory":"CAT","managementType":"INDIVIDUAL"}
                """);

        mockMvc.perform(delete("/api/pets/" + cat + "/medical-records/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDICAL_RECORD_NOT_FOUND"));
    }
}
