package com.petroom.event;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petroom.support.IntegrationTest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class EventIntegrationTest extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private String bearer;
    private long dog;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
        dog = createPet(bearer, DOG_JSON);
    }

    private void postJson(String url, String json) throws Exception {
        mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("의료 예정일·투약 예정일·유통기한·소진 예상일이 이벤트 4종으로 계산된다")
    void fourEventTypes() throws Exception {
        postJson("/api/pets/" + dog + "/medical-records", """
                {"kind":"VACCINATION","recordDate":"%s","content":"종합백신","nextDate":"%s"}
                """.formatted(TODAY, TODAY.plusDays(1)));
        postJson("/api/pets/" + dog + "/medical-records", """
                {"kind":"MEDICATION","recordDate":"%s","content":"심장사상충약","nextDate":"%s"}
                """.formatted(TODAY, TODAY.plusDays(20)));
        // 하루 100g, 남은 300g → 3일 뒤 소진, 유통기한 2일 뒤
        postJson("/api/foods", """
                {"name":"사료 A","expiryDate":"%s","remainingG":300,"perServingG":100,"timesPerDay":1,"petIds":[%d]}
                """.formatted(TODAY.plusDays(2), dog));

        mockMvc.perform(get("/api/events").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].type").value(containsInAnyOrder("MEDICAL", "TREATMENT", "EXPIRY", "REBUY")))
                .andExpect(jsonPath("$[0].type").value("MEDICAL"))
                .andExpect(jsonPath("$[0].title").value("코코 접종 · 종합백신"))
                .andExpect(jsonPath("$[0].daysUntil").value(1));

        // 기본 알림 시점(의료 1일, 치료 0일, 유통기한 3일, 재구매 5일): 투약(20일 뒤)만 제외
        mockMvc.perform(get("/api/events/alerts").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].type").value(containsInAnyOrder("MEDICAL", "EXPIRY", "REBUY")));

        // 알림 시점을 0일로 바꾸면 오늘 이벤트만 남는다 (모두 1일 이상 남음)
        mockMvc.perform(put("/api/users/me/settings").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicalNoticeDays":0,"treatmentNoticeDays":0,"expiryNoticeDays":0,"rebuyNoticeDays":0,"theme":"PASTEL"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/events/alerts").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("유통기한이 지난 먹이는 계속 알림 대상이다")
    void overdueExpiryAlerts() throws Exception {
        postJson("/api/foods", """
                {"name":"간식","type":"TREAT","expiryDate":"%s"}
                """.formatted(TODAY.minusDays(3)));
        mockMvc.perform(get("/api/events/alerts").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("EXPIRY"))
                .andExpect(jsonPath("$[0].daysUntil").value(-3));
    }

    @Test
    @DisplayName("달력은 이벤트와 기록이 있는 날만 담고, 하루 상세에서 그날의 기록을 돌려준다")
    void calendar() throws Exception {
        mockMvc.perform(put("/api/pets/" + dog + "/health-records/" + TODAY)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meal\":\"NORMAL\",\"stool\":\"NORMAL\"}"))
                .andExpect(status().isOk());
        postJson("/api/pets/" + dog + "/special-records", """
                {"item":"WALK","recordDate":"%s","value":"30분"}
                """.formatted(TODAY));

        String month = YearMonth.from(TODAY).toString();
        mockMvc.perform(get("/api/calendar").param("month", month).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(month))
                .andExpect(jsonPath("$.days.length()").value(1))
                .andExpect(jsonPath("$.days[0].date").value(TODAY.toString()))
                .andExpect(jsonPath("$.days[0].recordCount").value(2));

        mockMvc.perform(get("/api/calendar/days/" + TODAY).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.health.length()").value(1))
                .andExpect(jsonPath("$.special[0].petId").value(dog))
                .andExpect(jsonPath("$.special[*].item").value(hasItem("WALK")));
    }

    @Test
    @DisplayName("잘못된 월 형식과 366일을 넘는 기간은 400")
    void invalidRange() throws Exception {
        mockMvc.perform(get("/api/calendar").param("month", "2026-13").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/events").param("from", TODAY.toString()).param("to", TODAY.plusDays(400).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest());
    }
}
