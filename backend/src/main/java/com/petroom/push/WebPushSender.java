package com.petroom.push;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 실제 발송: 암호화한 본문을 VAPID 헤더와 함께 구독 endpoint로 POST */
public class WebPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);
    private static final int TTL_SECONDS = 24 * 3600;

    private final VapidSigner signer;
    private final RestClient client;

    public WebPushSender(PushProperties properties, Clock clock) throws GeneralSecurityException {
        this.signer = new VapidSigner(properties, clock);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public int send(PushSubscription s, String payloadJson) {
        try {
            byte[] body = WebPushEncryption.encrypt(payloadJson.getBytes(StandardCharsets.UTF_8),
                    EcKeys.b64(s.getP256dh()), EcKeys.b64(s.getAuth()));
            return client.post()
                    .uri(s.getEndpoint())
                    .header("Authorization", signer.authorization(s.getEndpoint()))
                    .header("Content-Encoding", "aes128gcm")
                    .header("TTL", String.valueOf(TTL_SECONDS))
                    .header("Urgency", "normal")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body)
                    .exchange((req, res) -> res.getStatusCode().value());
        } catch (GeneralSecurityException | IllegalArgumentException | RestClientException e) {
            log.warn("web push failed for subscription {}: {}", s.getId(), e.getMessage());
            return -1;
        }
    }
}
