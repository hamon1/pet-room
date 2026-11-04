package com.petroom.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** 빠른 기록·일괄 기록: 같은 날짜에 여러 마리의 상태를 한 번에 남깁니다. */
public record BatchHealthRequest(
        @NotNull(message = "날짜를 입력해 주세요.")
        LocalDate date,

        @NotEmpty(message = "기록할 반려동물을 선택해 주세요.")
        @Size(max = 50, message = "한 번에 50마리까지 기록할 수 있습니다.")
        List<@Valid Item> items
) {

    public record Item(
            @NotNull(message = "반려동물을 선택해 주세요.") Long petId,
            Condition meal,
            Condition stool,
            FeedStatus fed
    ) {
    }
}
