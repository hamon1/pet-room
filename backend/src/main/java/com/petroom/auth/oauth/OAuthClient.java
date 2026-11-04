package com.petroom.auth.oauth;

/** 인가 코드를 제공자의 액세스 토큰으로 바꾸고 사용자 정보를 가져옵니다. */
public interface OAuthClient {

    OAuthProfile fetchProfile(String code, String redirectUri);
}
