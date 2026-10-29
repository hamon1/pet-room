package com.petroom.event;

import java.time.LocalDate;
import java.util.List;

/**
 * 계산된 주요 이벤트. 저장하지 않고 의료 기록·먹이 재고에서 매번 도출합니다.
 *
 * @param daysUntil 오늘 기준 남은 일수 (지났으면 음수)
 * @param alert     사용자 알림 시점 설정 기준으로 지금 알려야 하는지
 */
public record EventResponse(
        EventType type,
        LocalDate date,
        long daysUntil,
        boolean alert,
        String title,
        List<Long> petIds,
        Long medicalRecordId,
        Long foodId
) {
}
