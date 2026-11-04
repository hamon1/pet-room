package com.petroom.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petroom.support.IntegrationTest;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PushIntegrationTest extends IntegrationTest {

    private static final String PUBLIC_KEY;
    private static final String PRIVATE_KEY;
    private static final String UA_P256DH;

    static {
        try {
            KeyPair vapid = WebPushEncryptionTest.p256();
            PUBLIC_KEY = EcKeys.b64(EcKeys.uncompressed((ECPublicKey) vapid.getPublic()));
            PRIVATE_KEY = EcKeys.b64(EcKeys.raw((ECPrivateKey) vapid.getPrivate()));
            UA_P256DH = EcKeys.b64(EcKeys.uncompressed((ECPublicKey) WebPushEncryptionTest.p256().getPublic()));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void push(DynamicPropertyRegistry registry) {
        registry.add("app.push.vapid-public-key", () -> PUBLIC_KEY);
        registry.add("app.push.vapid-private-key", () -> PRIVATE_KEY);
        registry.add("app.push.subject", () -> "mailto:ops@example.com");
    }

    @MockitoBean
    PushSender sender;

    @Autowired
    DailyPushNotifier notifier;

    @Autowired
    PushSubscriptionRepository subscriptions;

    private final List<String> sentTo = new ArrayList<>();
    private String bearer;
    private String endpoint;

    @BeforeEach
    void setUp() throws Exception {
        bearer = signupAndGetBearer();
        endpoint = "https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID();
        doAnswer(inv -> {
            sentTo.add(((PushSubscription) inv.getArgument(0)).getEndpoint());
            return 201;
        }).when(sender).send(any(), anyString());
    }

    private String subscription(String url) {
        return """
                {"endpoint":"%s","keys":{"p256dh":"%s","auth":"%s"}}
                """.formatted(url, UA_P256DH, EcKeys.b64(new byte[16]));
    }

    private void subscribe() throws Exception {
        mockMvc.perform(post("/api/push-subscriptions").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(subscription(endpoint)))
                .andExpect(status().isNoContent());
    }

    private void foodExpiringTomorrow() throws Exception {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        mockMvc.perform(post("/api/foods").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"사료 A\",\"expiryDate\":\"" + tomorrow + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("공개키를 받고, 허용된 푸시 서비스 주소와 올바른 키로만 구독할 수 있다")
    void subscribeValidation() throws Exception {
        mockMvc.perform(get("/api/push/public-key").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.publicKey").value(PUBLIC_KEY));
        mockMvc.perform(post("/api/push-subscriptions").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(subscription("https://attacker.example/x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PUSH_SUBSCRIPTION"));
        mockMvc.perform(post("/api/push-subscriptions").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"" + endpoint + "\",\"keys\":{\"p256dh\":\"AAAA\",\"auth\":\"AAAA\"}}"))
                .andExpect(status().isBadRequest());

        subscribe();
        subscribe(); // 같은 브라우저가 다시 구독해도 한 건
        assertThat(subscriptions.findByEndpoint(endpoint)).isPresent();

        mockMvc.perform(delete("/api/push-subscriptions").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"endpoint\":\"" + endpoint + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(subscriptions.findByEndpoint(endpoint)).isEmpty();
    }

    @Test
    @DisplayName("알림 대상이 있으면 하루 한 번만 보내고, 알림 대상이 없으면 보내지 않는다")
    void dailyOnce() throws Exception {
        subscribe();
        notifier.run();
        assertThat(sentTo).doesNotContain(endpoint); // 아직 이벤트 없음

        foodExpiringTomorrow();
        notifier.run();
        notifier.run();
        assertThat(sentTo).filteredOn(endpoint::equals).hasSize(1);
    }

    @Test
    @DisplayName("푸시 서비스가 410(구독 해지)을 돌려주면 구독을 지운다")
    void goneSubscriptionIsRemoved() throws Exception {
        subscribe();
        foodExpiringTomorrow();
        doReturn(410).when(sender).send(any(), anyString());
        notifier.run();
        assertThat(subscriptions.findByEndpoint(endpoint)).isEmpty();
    }

    @Test
    @DisplayName("알림 내용: 건수와 첫 이벤트 제목")
    void payload() {
        var e = new com.petroom.event.EventResponse(com.petroom.event.EventType.EXPIRY, LocalDate.now(), 1, true,
                "사료 \"A\" 유통기한", List.of(), null, 1L);
        assertThat(DailyPushNotifier.payload(List.of(e, e)))
                .isEqualTo("{\"title\":\"오늘 챙길 일 2건\",\"body\":\"사료 \\\"A\\\" 유통기한 외 1건\",\"url\":\"/events\"}");
    }
}
