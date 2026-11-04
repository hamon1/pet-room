package com.petroom.event;

import com.petroom.global.config.Today;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.user.CurrentUser;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 366;

    private final EventCalculator calculator;
    private final Today today;

    public EventController(EventCalculator calculator, Today today) {
        this.calculator = calculator;
        this.today = today;
    }

    /** 기간 내 주요 이벤트. 기본은 오늘부터 30일. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<EventResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from == null ? today.get() : from;
        LocalDate end = to == null ? start.plusDays(DEFAULT_DAYS) : to;
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "기간은 시작일부터 366일 이내로 정해 주세요.");
        }
        return calculator.between(CurrentUser.id(jwt), start, end);
    }

    /** 알림 시점 설정 기준으로 지금 알려야 하는 이벤트 (메인 화면 "주요 이벤트") */
    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public List<EventResponse> alerts(@AuthenticationPrincipal Jwt jwt) {
        return calculator.alerts(CurrentUser.id(jwt));
    }
}
