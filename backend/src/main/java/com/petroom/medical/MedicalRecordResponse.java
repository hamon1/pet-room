package com.petroom.medical;

import java.time.LocalDate;

public record MedicalRecordResponse(
        Long id,
        Long petId,
        MedicalKind kind,
        LocalDate recordDate,
        String hospital,
        String content,
        LocalDate nextDate
) {

    public static MedicalRecordResponse from(MedicalRecord r) {
        return new MedicalRecordResponse(r.getId(), r.getPetId(), r.getKind(), r.getRecordDate(), r.getHospital(),
                r.getContent(), r.getNextDate());
    }
}
