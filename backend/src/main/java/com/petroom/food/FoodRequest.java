package com.petroom.food;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 필수는 제품명·유통기한 두 가지뿐이고 나머지는 선택입니다 (사료 입력 간소화). */
public record FoodRequest(
        @NotBlank(message = "제품명을 입력해 주세요.")
        @Size(max = 50, message = "제품명은 50자 이내로 입력해 주세요.")
        String name,

        @NotNull(message = "유통기한을 입력해 주세요.")
        LocalDate expiryDate,

        FoodType type,

        LocalDate openedDate,

        @DecimalMin(value = "0", message = "양은 0 이상이어야 합니다.")
        @DecimalMax(value = "99999999", message = "양을 확인해 주세요.")
        BigDecimal totalG,

        @DecimalMin(value = "0", message = "남은 양은 0 이상이어야 합니다.")
        @DecimalMax(value = "99999999", message = "남은 양을 확인해 주세요.")
        BigDecimal remainingG,

        @DecimalMin(value = "0.1", message = "1회 급여량은 0보다 커야 합니다.")
        @DecimalMax(value = "999999", message = "1회 급여량을 확인해 주세요.")
        BigDecimal perServingG,

        @Min(value = 1, message = "하루 급여 횟수는 1 이상이어야 합니다.")
        @Max(value = 24, message = "하루 급여 횟수를 확인해 주세요.")
        Integer timesPerDay,

        @Size(max = 50, message = "먹는 동물은 50마리까지 선택할 수 있습니다.")
        List<Long> petIds
) {

    public FoodType typeOrDefault() {
        return type == null ? FoodType.FEED : type;
    }
}
