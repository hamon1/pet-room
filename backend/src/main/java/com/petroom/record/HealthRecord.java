package com.petroom.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 반려동물 한 마리의 하루 건강 기록. (pet_id, record_date)당 하나만 존재합니다. */
@Entity
@Table(name = "health_records")
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private Long petId;

    @Column(name = "record_date", nullable = false, updatable = false)
    private LocalDate recordDate;

    @Column(name = "weight_kg", precision = 6, scale = 3)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Condition meal;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Condition stool;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedStatus fed;

    @Column(length = 500)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HealthRecord() {
    }

    public HealthRecord(Long petId, LocalDate recordDate) {
        this.petId = petId;
        this.recordDate = recordDate;
    }

    /** 전체 덮어쓰기: 보내지 않은 값은 비웁니다. */
    public void replace(HealthValues v) {
        this.weightKg = v.weightKg();
        this.meal = v.meal();
        this.stool = v.stool();
        this.fed = v.fed();
        this.memo = v.memo();
    }

    /** 부분 반영(빠른 기록): 보낸 값만 바꾸고 체중·메모 등 나머지는 유지합니다. */
    public void merge(HealthValues v) {
        if (v.weightKg() != null) this.weightKg = v.weightKg();
        if (v.meal() != null) this.meal = v.meal();
        if (v.stool() != null) this.stool = v.stool();
        if (v.fed() != null) this.fed = v.fed();
        if (v.memo() != null) this.memo = v.memo();
    }

    public Long getId() {
        return id;
    }

    public Long getPetId() {
        return petId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public Condition getMeal() {
        return meal;
    }

    public Condition getStool() {
        return stool;
    }

    public FeedStatus getFed() {
        return fed;
    }

    public String getMemo() {
        return memo;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
