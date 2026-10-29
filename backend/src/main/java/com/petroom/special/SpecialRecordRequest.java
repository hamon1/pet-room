package com.petroom.special;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SpecialRecordRequest(
        @NotNull(message = "항목을 선택해 주세요.")
        SpecialItem item,

        @NotNull(message = "날짜를 입력해 주세요.")
        LocalDate recordDate,

        @Size(max = 100, message = "내용은 100자 이내로 입력해 주세요.")
        String value
) {
}
