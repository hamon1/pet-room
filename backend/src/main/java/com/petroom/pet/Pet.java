package com.petroom.pet;

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
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_category", length = 20)
    private PetSubCategory subCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "management_type", nullable = false, length = 20)
    private ManagementType managementType;

    @Column(length = 30)
    private String species;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_date_estimated", nullable = false)
    private boolean birthDateEstimated;

    @Column(length = 500)
    private String traits;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pet() {
    }

    public Pet(Long ownerId, PetProfile profile) {
        this.ownerId = ownerId;
        apply(profile);
    }

    public void update(PetProfile profile) {
        apply(profile);
    }

    private void apply(PetProfile p) {
        PetClassification.validate(p.category(), p.subCategory(), p.managementType());
        this.name = p.name();
        this.category = p.category();
        this.subCategory = p.subCategory();
        this.managementType = p.managementType();
        this.species = p.species();
        this.birthDate = p.birthDate();
        this.birthDateEstimated = p.birthDateEstimated();
        this.traits = p.traits();
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public PetCategory getCategory() {
        return category;
    }

    public PetSubCategory getSubCategory() {
        return subCategory;
    }

    public ManagementType getManagementType() {
        return managementType;
    }

    public String getSpecies() {
        return species;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isTank() {
        return managementType == ManagementType.TANK;
    }

    public boolean isBirthDateEstimated() {
        return birthDateEstimated;
    }

    public String getTraits() {
        return traits;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
