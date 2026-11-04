package com.petroom.record;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HealthRecordResponse(
        Long petId,
        LocalDate date,
        BigDecimal weightKg,
        Condition meal,
        Condition stool,
        FeedStatus fed,
        String memo
) {

    public static HealthRecordResponse from(HealthRecord r) {
        return new HealthRecordResponse(r.getPetId(), r.getRecordDate(), r.getWeightKg(), r.getMeal(),
                r.getStool(), r.getFed(), r.getMemo());
    }
}
