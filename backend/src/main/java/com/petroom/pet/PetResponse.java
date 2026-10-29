package com.petroom.pet;

import java.time.Instant;
import java.time.LocalDate;

public record PetResponse(
        Long id,
        String name,
        PetCategory category,
        PetSubCategory subCategory,
        ManagementType managementType,
        String species,
        LocalDate birthDate,
        boolean birthDateEstimated,
        String traits,
        Instant createdAt
) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getCategory(),
                pet.getSubCategory(),
                pet.getManagementType(),
                pet.getSpecies(),
                pet.getBirthDate(),
                pet.isBirthDateEstimated(),
                pet.getTraits(),
                pet.getCreatedAt());
    }
}
