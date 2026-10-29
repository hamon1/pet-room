package com.petroom.tank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TankFishRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이내로 입력해 주세요.")
        String name,

        @Size(max = 30, message = "종명은 30자 이내로 입력해 주세요.")
        String species
) {
}
