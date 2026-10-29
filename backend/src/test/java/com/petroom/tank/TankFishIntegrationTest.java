package com.petroom.tank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petroom.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class TankFishIntegrationTest extends IntegrationTest {

    private String bearer;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
    }

    @Test
    @DisplayName("어항에 물고기를 추가하면 종명을 비워도 어항의 종을 따른다")
    void addAndRemove() throws Exception {
        long tank = createPet(bearer, TANK_JSON);
        String response = mockMvc.perform(post("/api/pets/" + tank + "/fish")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"구피 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.species").value("구피"))
                .andReturn().getResponse().getContentAsString();
        long fishId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/api/pets/" + tank + "/fish").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/pets/" + tank + "/fish/" + fishId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("어항이 아닌 반려동물에는 물고기를 넣을 수 없다")
    void notATank() throws Exception {
        long dog = createPet(bearer, DOG_JSON);
        mockMvc.perform(post("/api/pets/" + dog + "/fish")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"구피 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_A_TANK"));
    }
}
