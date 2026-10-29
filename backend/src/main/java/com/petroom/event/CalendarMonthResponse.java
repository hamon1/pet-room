package com.petroom.event;

import java.util.List;

/** 이벤트나 기록이 있는 날만 담습니다. */
public record CalendarMonthResponse(String month, List<CalendarDay> days) {
}
