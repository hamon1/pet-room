package com.petroom.medical;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private Long petId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicalKind kind;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(length = 50)
    private String hospital;

    @Column(nullable = false, length = 100)
    private String content;

    @Column(name = "next_date")
    private LocalDate nextDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicalRecord() {
    }

    public MedicalRecord(Long petId) {
        this.petId = petId;
    }

    public void apply(MedicalKind kind, LocalDate recordDate, String hospital, String content, LocalDate nextDate) {
        this.kind = kind;
        this.recordDate = recordDate;
        this.hospital = hospital;
        this.content = content;
        this.nextDate = nextDate;
    }

    public Long getId() {
        return id;
    }

    public Long getPetId() {
        return petId;
    }

    public MedicalKind getKind() {
        return kind;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getHospital() {
        return hospital;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getNextDate() {
        return nextDate;
    }
}
