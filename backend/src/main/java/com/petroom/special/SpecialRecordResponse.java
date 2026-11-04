package com.petroom.special;

import java.time.LocalDate;

public record SpecialRecordResponse(Long id, Long petId, SpecialItem item, String label, LocalDate recordDate, String value) {

    public static SpecialRecordResponse from(SpecialRecord r) {
        return new SpecialRecordResponse(r.getId(), r.getPetId(), r.getItem(), r.getItem().label(), r.getRecordDate(), r.getValue());
    }
}
