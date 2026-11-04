package com.petroom.food;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class FoodIntegrationTest extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private String bearer;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
    }

    private long createFood(String json) throws Exception {
        String res = mockMvc.perform(post("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    @DisplayName("두 마리가 함께 먹는 사료: 하루 사용량과 소진 예상일을 계산하고 급여하면 두 마리분이 줄어든다")
    void sharedFood() throws Exception {
        long dog = createPet(bearer, DOG_JSON);
        long bird = createPet(bearer, BIRD_JSON);
        long id = createFood("""
                {"name":"사료 A","expiryDate":"%s","remainingG":1000,"perServingG":50,"timesPerDay":2,"petIds":[%d,%d]}
                """.formatted(TODAY.plusMonths(6), dog, bird));

        // 하루 50 × 2회 × 2마리 = 200g → 1000g은 5일
        mockMvc.perform(get("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].type").value("FEED"))
                .andExpect(jsonPath("$[0].totalG").value(1000))
                .andExpect(jsonPath("$[0].dailyUseG").value(200))
                .andExpect(jsonPath("$[0].runoutDate").value(TODAY.plusDays(5).toString()))
                .andExpect(jsonPath("$[0].petIds.length()").value(2));

        mockMvc.perform(post("/api/foods/" + id + "/feed").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingG").value(900));

        mockMvc.perform(patch("/api/foods/" + id + "/remaining").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remainingG\":30}"))
                .andExpect(jsonPath("$.remainingG").value(30));

        // 남은 양보다 많이 먹으면 0에서 멈춘다
        mockMvc.perform(post("/api/foods/" + id + "/feed").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.remainingG").value(0));

        mockMvc.perform(delete("/api/foods/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("제품명과 유통기한만으로 등록할 수 있고, 급여량이 없으면 급여 차감은 400")
    void minimalFood() throws Exception {
        long id = createFood("""
                {"name":"간식 B","type":"TREAT","expiryDate":"%s"}
                """.formatted(TODAY.plusDays(10)));
        mockMvc.perform(get("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].runoutDate").doesNotExist());
        mockMvc.perform(post("/api/foods/" + id + "/feed").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FOOD"));
    }

    @Test
    @DisplayName("다른 사용자의 반려동물을 연결하거나 다른 사용자의 먹이를 수정하면 404")
    void ownership() throws Exception {
        String other = signupAndGetBearer();
        long otherDog = createPet(other, DOG_JSON);
        mockMvc.perform(post("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"사료","expiryDate":"%s","petIds":[%d]}
                                """.formatted(TODAY.plusDays(30), otherDog)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));

        long id = createFood("""
                {"name":"사료","expiryDate":"%s"}
                """.formatted(TODAY.plusDays(30)));
        mockMvc.perform(delete("/api/foods/" + id).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOOD_NOT_FOUND"));
    }

    @Test
    @DisplayName("남은 양이 전체 양보다 많으면 400")
    void remainingOverTotal() throws Exception {
        mockMvc.perform(post("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"사료","expiryDate":"%s","totalG":100,"remainingG":200}
                                """.formatted(TODAY.plusDays(30))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FOOD"));
    }
}
