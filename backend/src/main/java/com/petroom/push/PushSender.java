package com.petroom.push;

/** 푸시 서비스로 한 건을 보냅니다. 테스트에서는 가짜로 바꿉니다. */
public interface PushSender {

    /** @return 푸시 서비스의 HTTP 상태 코드, 연결 실패 시 -1 */
    int send(PushSubscription subscription, String payloadJson);
}
