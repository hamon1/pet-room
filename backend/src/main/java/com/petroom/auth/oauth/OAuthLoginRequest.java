package com.petroom.auth.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(
        @NotBlank(message = "인가 코드가 필요합니다.")
        @Size(max = 2048, message = "인가 코드 형식이 아닙니다.")
        String code,

        @NotBlank(message = "콜백 주소가 필요합니다.")
        @Size(max = 300, message = "콜백 주소가 너무 깁니다.")
        String redirectUri
) {
}
