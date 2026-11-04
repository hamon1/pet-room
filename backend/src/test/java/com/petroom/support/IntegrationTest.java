package com.petroom.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 실제 PostgreSQL(Testcontainers)로 실행하는 통합 테스트의 공통 설정.
 * 컨테이너는 테스트 전체에서 한 번만 띄워 재사용합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    public static final String DOG_JSON = """
            {"name":"코코","category":"MAMMAL","subCategory":"DOG","managementType":"INDIVIDUAL"}
            """;
    public static final String BIRD_JSON = """
            {"name":"삐삐","category":"BIRD","subCategory":"PARROT","managementType":"INDIVIDUAL"}
            """;
    public static final String TANK_JSON = """
            {"name":"어항 1","category":"FISH","managementType":"TANK","species":"구피"}
            """;

    /** 반려동물을 등록하고 id를 돌려줍니다. */
    protected long createPet(String bearer, String json) throws Exception {
        String response = mockMvc.perform(post("/api/pets")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    /** 새 사용자를 가입시키고 Authorization 헤더 값을 돌려줍니다. */
    protected String signupAndGetBearer() throws Exception {
        String body = """
                {"email":"%s","password":"password123","nickname":"테스터"}
                """.formatted(uniqueEmail());
        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(response, "$.accessToken");
    }
}
