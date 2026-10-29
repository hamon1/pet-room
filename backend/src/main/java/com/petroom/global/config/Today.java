package com.petroom.global.config;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** 서비스 기준(한국 시간) 오늘 날짜. 요청 검증의 @PastOrPresent는 서버 시간대를 따르므로 날짜 규칙은 여기서 확인합니다. */
@Component
public class Today {

    private final Clock clock;

    public Today(Clock clock) {
        this.clock = clock;
    }

    public LocalDate get() {
        return LocalDate.now(clock);
    }
}
