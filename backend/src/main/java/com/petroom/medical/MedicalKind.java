package com.petroom.medical;

/** 진료 / 접종 / 투약 / 치료. 투약·치료는 달력에서 "치료 정보", 진료·접종은 "의료 일정"으로 묶입니다. */
public enum MedicalKind {
    VISIT,
    VACCINATION,
    MEDICATION,
    TREATMENT;

    public boolean isTreatment() {
        return this == MEDICATION || this == TREATMENT;
    }
}
