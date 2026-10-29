package com.petroom.pet;

import java.time.LocalDate;

/** 검증과 정리가 끝난 반려동물 정보. 요청 DTO와 엔티티 사이의 값 객체입니다. */
public record PetProfile(
        String name,
        PetCategory category,
        PetSubCategory subCategory,
        ManagementType managementType,
        String species,
        LocalDate birthDate,
        boolean birthDateEstimated,
        String traits
) {
}
