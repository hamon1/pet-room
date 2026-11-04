package com.petroom.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "리프레시 토큰이 필요합니다.")
        @Size(max = 200, message = "리프레시 토큰 형식이 아닙니다.")
        String refreshToken
) {
}
