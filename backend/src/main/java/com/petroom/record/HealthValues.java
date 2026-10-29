package com.petroom.record;

import java.math.BigDecimal;

public record HealthValues(BigDecimal weightKg, Condition meal, Condition stool, FeedStatus fed, String memo) {

    public boolean isEmpty() {
        return weightKg == null && meal == null && stool == null && fed == null && memo == null;
    }
}
