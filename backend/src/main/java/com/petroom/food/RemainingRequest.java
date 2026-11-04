package com.petroom.food;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RemainingRequest(
        @NotNull(message = "남은 양을 입력해 주세요.")
        @DecimalMin(value = "0", message = "남은 양은 0 이상이어야 합니다.")
        @DecimalMax(value = "99999999", message = "남은 양을 확인해 주세요.")
        BigDecimal remainingG
) {
}
