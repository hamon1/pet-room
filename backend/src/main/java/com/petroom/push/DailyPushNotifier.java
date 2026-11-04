package com.petroom.push;

import com.petroom.event.EventCalculator;
import com.petroom.event.EventResponse;
import com.petroom.global.config.Today;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 정해진 시각에, 알림 대상 이벤트가 있는 사용자에게 요약 알림을 한 번 보냅니다.
 * 알림 대상 판정은 화면의 "주요 이벤트"와 같은 계산(EventCalculator.alerts)을 씁니다.
 */
@Component
public class DailyPushNotifier {

    private static final Logger log = LoggerFactory.getLogger(DailyPushNotifier.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final PushProperties properties;
    private final PushSubscriptionRepository repository;
    private final PushSender sender;
    private final EventCalculator events;
    private final Today today;

    public DailyPushNotifier(PushProperties properties, PushSubscriptionRepository repository, PushSender sender,
                             EventCalculator events, Today today) {
        this.properties = properties;
        this.repository = repository;
        this.sender = sender;
        this.events = events;
        this.today = today;
    }

    @Scheduled(cron = "${app.push.cron}", zone = "Asia/Seoul")
    public void scheduled() {
        if (properties.enabled()) {
            run();
        }
    }

    /** @return 알림을 보낸 사용자 수 */
    public int run() {
        LocalDate date = today.get();
        int notified = 0;
        for (Long userId : repository.findSubscribedUserIds()) {
            try {
                if (notify(userId, date)) {
                    notified++;
                }
            } catch (RuntimeException e) {
                log.error("daily push failed for user {}", userId, e);
            }
        }
        log.info("daily push: {} users notified", notified);
        return notified;
    }

    private boolean notify(Long userId, LocalDate date) {
        List<EventResponse> alerts = events.alerts(userId);
        if (alerts.isEmpty() || repository.markDelivered(userId, date) == 0) {
            return false;
        }
        String payload = payload(alerts);
        for (PushSubscription s : repository.findAllByUserId(userId)) {
            int status = sender.send(s, payload);
            // 404·410: 브라우저가 구독을 해지함 → 정리
            if (status == 404 || status == 410) {
                repository.delete(s);
            }
        }
        return true;
    }

    static String payload(List<EventResponse> alerts) {
        String body = alerts.size() == 1
                ? alerts.getFirst().title()
                : alerts.getFirst().title() + " 외 " + (alerts.size() - 1) + "건";
        Map<String, String> message = new LinkedHashMap<>();
        message.put("title", "오늘 챙길 일 " + alerts.size() + "건");
        message.put("body", body);
        message.put("url", "/events");
        try {
            return JSON.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
