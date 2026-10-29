package com.petroom.special;

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

@Entity
@Table(name = "special_records")
public class SpecialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private Long petId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_key", nullable = false, length = 30)
    private SpecialItem item;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(length = 100)
    private String value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SpecialRecord() {
    }

    public SpecialRecord(Long petId, SpecialItem item, LocalDate recordDate, String value) {
        this.petId = petId;
        this.item = item;
        this.recordDate = recordDate;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Long getPetId() {
        return petId;
    }

    public SpecialItem getItem() {
        return item;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getValue() {
        return value;
    }
}
