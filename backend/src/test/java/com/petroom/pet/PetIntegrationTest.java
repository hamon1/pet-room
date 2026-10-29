package com.petroom.pet;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petroom.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PetIntegrationTest extends IntegrationTest {

    private static final String DOG = """
            {"name":"코코","category":"MAMMAL","subCategory":"DOG","managementType":"INDIVIDUAL",
             "species":"말티즈","birthDate":"2023-05-01","traits":"산책을 좋아함"}
            """;

    private String bearer;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
    }

    private ResultActions create(String auth, String body) throws Exception {
        return mockMvc.perform(post("/api/pets")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long createdId(String auth, String body) throws Exception {
        String response = create(auth, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    @Test
    @DisplayName("강아지를 등록하면 201과 Location을 돌려준다")
    void createDog() throws Exception {
        create(bearer, DOG)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/pets/")))
                .andExpect(jsonPath("$.name").value("코코"))
                .andExpect(jsonPath("$.subCategory").value("DOG"))
                .andExpect(jsonPath("$.birthDateEstimated").value(false));
    }

    @Test
    @DisplayName("어항은 세부 분류 없이 어항 단위로 등록한다")
    void createTank() throws Exception {
        create(bearer, """
                {"name":"어항 1","category":"FISH","managementType":"TANK","species":"구피"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managementType").value("TANK"))
                .andExpect(jsonPath("$.subCategory").doesNotExist());
    }

    @Test
    @DisplayName("분류와 맞지 않는 세부 분류나 관리 방식은 400")
    void invalidClassification() throws Exception {
        create(bearer, """
                {"name":"삐삐","category":"MAMMAL","subCategory":"PARROT","managementType":"INDIVIDUAL"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PET_CLASSIFICATION"));

        create(bearer, """
                {"name":"코코","category":"MAMMAL","subCategory":"DOG","managementType":"TANK"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PET_CLASSIFICATION"));
    }

    @Test
    @DisplayName("나이로 입력하면 출생일을 추정해 저장한다")
    void ageYears() throws Exception {
        create(bearer, """
                {"name":"나비","category":"MAMMAL","subCategory":"CAT","managementType":"INDIVIDUAL","ageYears":2}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.birthDateEstimated").value(true))
                .andExpect(jsonPath("$.birthDate").isNotEmpty());
    }

    @Test
    @DisplayName("없는 분류 값이면 400")
    void unknownEnum() throws Exception {
        create(bearer, """
                {"name":"도마","category":"REPTILE","managementType":"INDIVIDUAL"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("목록에는 내 반려동물만 보이고, 남의 반려동물은 404")
    void ownership() throws Exception {
        long myPet = createdId(bearer, DOG);
        String other = signupAndGetBearer();

        mockMvc.perform(get("/api/pets").header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/pets/" + myPet).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));

        mockMvc.perform(delete("/api/pets/" + myPet).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/pets").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("수정 후 삭제하면 조회되지 않는다")
    void updateThenDelete() throws Exception {
        long id = createdId(bearer, DOG);

        mockMvc.perform(put("/api/pets/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DOG.replace("코코", "코코아")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("코코아"));

        mockMvc.perform(delete("/api/pets/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pets/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
    }
}
