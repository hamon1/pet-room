package com.petroom.event;

/** 달력에 강조되는 주요 이벤트 4종 */
public enum EventType {
    /** 진료·접종의 다음 예정일 */
    MEDICAL,
    /** 투약·치료의 다음 예정일 */
    TREATMENT,
    /** 먹이 유통기한 */
    EXPIRY,
    /** 먹이 소진 예상일 (재구매 필요) */
    REBUY
}
