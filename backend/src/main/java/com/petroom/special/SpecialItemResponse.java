package com.petroom.special;

/** 반려동물에게 해당하는 특수 항목과 가장 최근 기록. */
public record SpecialItemResponse(SpecialItem item, String label, boolean userDefined, SpecialRecordResponse latest) {
}
