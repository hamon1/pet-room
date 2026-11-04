package com.petroom.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtConfigTest {

    private final JwtConfig config = new JwtConfig();

    private static JwtProperties props(String secret) {
        return new JwtProperties(secret, 1800, 1209600, "pet-room");
    }

    @Test
    @DisplayName("운영(prod)에서 로컬 개발용 기본 비밀키를 쓰면 시작하지 않는다")
    void prodRejectsDevSecret() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> config.jwtSecretKey(props(JwtConfig.DEV_SECRET), prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatCode(() -> config.jwtSecretKey(props("x".repeat(48)), prod)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("로컬에서는 기본 비밀키를 허용하고, 32바이트 미만은 어디서나 거절한다")
    void devAllowsDefault() {
        MockEnvironment dev = new MockEnvironment();
        assertThatCode(() -> config.jwtSecretKey(props(JwtConfig.DEV_SECRET), dev)).doesNotThrowAnyException();
        assertThatThrownBy(() -> config.jwtSecretKey(props("short"), dev)).isInstanceOf(IllegalStateException.class);
    }
}
