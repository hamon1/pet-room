package com.petroom.push;

import java.security.GeneralSecurityException;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class PushConfig {

    /** 키가 없으면 보내지 않는 발송기를 둡니다 (웹 푸시 비활성). */
    @Bean
    public PushSender pushSender(PushProperties properties, Clock clock) throws GeneralSecurityException {
        if (!properties.enabled()) {
            return (subscription, payload) -> -1;
        }
        return new WebPushSender(properties, clock);
    }
}
