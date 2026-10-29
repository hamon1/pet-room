package com.petroom.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MedicalRecordRequest(
        @NotNull(message = "종류를 선택해 주세요.")
        MedicalKind kind,

        @NotNull(message = "날짜를 입력해 주세요.")
        LocalDate recordDate,

        @Size(max = 50, message = "병원명은 50자 이내로 입력해 주세요.")
        String hospital,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 100, message = "내용은 100자 이내로 입력해 주세요.")
        String content,

        LocalDate nextDate
) {
}
