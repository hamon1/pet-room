package com.petroom.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param vapidPublicKey  P-256 공개키 (압축하지 않은 65바이트, base64url)
 * @param vapidPrivateKey P-256 개인키 (32바이트, base64url)
 * @param subject         푸시 서비스가 연락할 운영자 주소 (mailto: 또는 https:)
 * @param cron            매일 알림 시각 (한국 시간)
 */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(String vapidPublicKey, String vapidPrivateKey, String subject, String cron) {

    public boolean enabled() {
        return notBlank(vapidPublicKey) && notBlank(vapidPrivateKey) && notBlank(subject);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
