package com.petroom.record;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record HealthRecordRequest(
        @DecimalMin(value = "0.001", message = "체중은 0보다 커야 합니다.")
        @DecimalMax(value = "200", message = "체중을 확인해 주세요.")
        @Digits(integer = 3, fraction = 3, message = "체중은 소수점 셋째 자리까지 입력할 수 있습니다.")
        BigDecimal weightKg,
        Condition meal,
        Condition stool,
        FeedStatus fed,
        @Size(max = 500, message = "메모는 500자 이내로 입력해 주세요.")
        String memo
) {

    public HealthValues toValues() {
        String trimmed = memo == null || memo.isBlank() ? null : memo.strip();
        return new HealthValues(weightKg, meal, stool, fed, trimmed);
    }
}
