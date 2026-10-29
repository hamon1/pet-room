package com.petroom.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    INVALID_PET_CLASSIFICATION(HttpStatus.BAD_REQUEST, "분류, 세부 분류, 관리 방식 조합을 확인해 주세요."),
    INVALID_RECORD(HttpStatus.BAD_REQUEST, "기록 내용을 확인해 주세요."),
    INVALID_SPECIAL_ITEM(HttpStatus.BAD_REQUEST, "이 반려동물에게 없는 특수 항목입니다."),
    NOT_A_TANK(HttpStatus.BAD_REQUEST, "어항 단위로 관리하는 반려동물이 아닙니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 맞지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),
    HEALTH_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 날짜의 기록이 없습니다."),
    MEDICAL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "의료 기록을 찾을 수 없습니다."),
    SPECIAL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "특수 항목 기록을 찾을 수 없습니다."),
    FISH_NOT_FOUND(HttpStatus.NOT_FOUND, "물고기를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
