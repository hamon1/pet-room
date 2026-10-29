package com.petroom.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 알림 시점: 이벤트 며칠 전부터 알림 대상으로 볼지 (0 = 당일) */
public record SettingsRequest(
        @NotNull @Min(value = 0, message = "0일 이상이어야 합니다.") @Max(value = 30, message = "30일 이하로 정해 주세요.")
        Integer medicalNoticeDays,
        @NotNull @Min(value = 0, message = "0일 이상이어야 합니다.") @Max(value = 30, message = "30일 이하로 정해 주세요.")
        Integer treatmentNoticeDays,
        @NotNull @Min(value = 0, message = "0일 이상이어야 합니다.") @Max(value = 30, message = "30일 이하로 정해 주세요.")
        Integer expiryNoticeDays,
        @NotNull @Min(value = 0, message = "0일 이상이어야 합니다.") @Max(value = 30, message = "30일 이하로 정해 주세요.")
        Integer rebuyNoticeDays,
        @NotNull(message = "테마를 선택해 주세요.")
        Theme theme
) {
}
