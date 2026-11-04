package com.petroom.auth.oauth;

/** 소셜 로그인 제공자가 돌려준 사용자 정보. 이메일은 동의하지 않으면 없을 수 있습니다. */
public record OAuthProfile(String providerUserId, String email, boolean emailVerified, String nickname) {
}
