package com.petroom.event;

import java.time.LocalDate;
import java.util.List;

/** 달력 한 칸: 이벤트 종류(강조 표시용)와 그날 남긴 기록 수 */
public record CalendarDay(LocalDate date, List<EventType> eventTypes, int recordCount) {
}
