package com.petroom.user;

import org.springframework.security.oauth2.jwt.Jwt;

/** JWT의 sub(사용자 id)를 꺼내는 도우미. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
