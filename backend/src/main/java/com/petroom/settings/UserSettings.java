package com.petroom.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    /** 기획서의 기본안: 의료 1일 전, 치료 당일, 유통기한 3일 전, 재구매 5일 전, 파스텔 테마 */
    public static final int DEFAULT_MEDICAL = 1;
    public static final int DEFAULT_TREATMENT = 0;
    public static final int DEFAULT_EXPIRY = 3;
    public static final int DEFAULT_REBUY = 5;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "medical_notice_days", nullable = false)
    private int medicalNoticeDays;

    @Column(name = "treatment_notice_days", nullable = false)
    private int treatmentNoticeDays;

    @Column(name = "expiry_notice_days", nullable = false)
    private int expiryNoticeDays;

    @Column(name = "rebuy_notice_days", nullable = false)
    private int rebuyNoticeDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Theme theme;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSettings() {
    }

    public static UserSettings defaults(Long userId) {
        UserSettings s = new UserSettings();
        s.userId = userId;
        s.medicalNoticeDays = DEFAULT_MEDICAL;
        s.treatmentNoticeDays = DEFAULT_TREATMENT;
        s.expiryNoticeDays = DEFAULT_EXPIRY;
        s.rebuyNoticeDays = DEFAULT_REBUY;
        s.theme = Theme.PASTEL;
        return s;
    }

    public void update(SettingsRequest r) {
        this.medicalNoticeDays = r.medicalNoticeDays();
        this.treatmentNoticeDays = r.treatmentNoticeDays();
        this.expiryNoticeDays = r.expiryNoticeDays();
        this.rebuyNoticeDays = r.rebuyNoticeDays();
        this.theme = r.theme();
    }

    public int getMedicalNoticeDays() {
        return medicalNoticeDays;
    }

    public int getTreatmentNoticeDays() {
        return treatmentNoticeDays;
    }

    public int getExpiryNoticeDays() {
        return expiryNoticeDays;
    }

    public int getRebuyNoticeDays() {
        return rebuyNoticeDays;
    }

    public Theme getTheme() {
        return theme;
    }
}
