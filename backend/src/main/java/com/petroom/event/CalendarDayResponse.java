package com.petroom.event;

import com.petroom.medical.MedicalRecordResponse;
import com.petroom.record.HealthRecordResponse;
import com.petroom.special.SpecialRecordResponse;
import java.time.LocalDate;
import java.util.List;

public record CalendarDayResponse(
        LocalDate date,
        List<EventResponse> events,
        List<HealthRecordResponse> health,
        List<MedicalRecordResponse> medical,
        List<SpecialRecordResponse> special
) {
}
