package com.petroom.auth.oauth;

import java.util.Locale;

public enum OAuthProvider {
    KAKAO,
    GOOGLE;

    /** 경로의 소문자 이름(kakao, google)을 받아들입니다. */
    public static OAuthProvider fromPath(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
