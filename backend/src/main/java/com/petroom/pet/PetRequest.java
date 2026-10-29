package com.petroom.pet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이내로 입력해 주세요.")
        String name,

        @NotNull(message = "분류를 선택해 주세요.")
        PetCategory category,

        PetSubCategory subCategory,

        @NotNull(message = "관리 방식을 선택해 주세요.")
        ManagementType managementType,

        @Size(max = 30, message = "상세 종명은 30자 이내로 입력해 주세요.")
        String species,

        @PastOrPresent(message = "출생일은 오늘 이후일 수 없습니다.")
        LocalDate birthDate,

        @DecimalMin(value = "0", message = "나이는 0 이상이어야 합니다.")
        @DecimalMax(value = "100", message = "나이를 확인해 주세요.")
        BigDecimal ageYears,

        @Size(max = 500, message = "특징은 500자 이내로 입력해 주세요.")
        String traits
) {
}
