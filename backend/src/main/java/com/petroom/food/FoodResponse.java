package com.petroom.food;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FoodResponse(
        Long id,
        String name,
        FoodType type,
        LocalDate expiryDate,
        LocalDate openedDate,
        BigDecimal totalG,
        BigDecimal remainingG,
        BigDecimal perServingG,
        Integer timesPerDay,
        List<Long> petIds,
        BigDecimal dailyUseG,
        LocalDate runoutDate
) {

    public static FoodResponse from(Food f, LocalDate today) {
        return new FoodResponse(f.getId(), f.getName(), f.getType(), f.getExpiryDate(), f.getOpenedDate(),
                f.getTotalG(), f.getRemainingG(), f.getPerServingG(), f.getTimesPerDay(),
                f.getPetIds().stream().sorted().toList(), f.dailyUse(), f.runoutDate(today));
    }
}
