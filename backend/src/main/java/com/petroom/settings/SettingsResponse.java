package com.petroom.settings;

public record SettingsResponse(
        int medicalNoticeDays,
        int treatmentNoticeDays,
        int expiryNoticeDays,
        int rebuyNoticeDays,
        Theme theme
) {

    public static SettingsResponse from(UserSettings s) {
        return new SettingsResponse(s.getMedicalNoticeDays(), s.getTreatmentNoticeDays(), s.getExpiryNoticeDays(),
                s.getRebuyNoticeDays(), s.getTheme());
    }
}
