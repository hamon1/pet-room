package com.petroom.special;

import com.petroom.pet.Pet;
import com.petroom.pet.PetCategory;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 분류별 특수 항목.
 * userDefined = 기획 단계에서 사용자가 직접 정한 항목, false = 검증 전 제안 항목(출시 전 전문가 확인 필요).
 */
public enum SpecialItem {
    NAIL("발톱 관리", Set.of(PetCategory.MAMMAL, PetCategory.BIRD), false),
    COAT_SKIN("털·피부 관리", Set.of(PetCategory.MAMMAL), false),
    DENTAL("치아 관리", Set.of(PetCategory.MAMMAL), false),
    WALK("산책 기록", Set.of(PetCategory.MAMMAL), false),
    BEAK("부리 관리", Set.of(PetCategory.BIRD), true),
    RESPIRATORY("호흡기 관리", Set.of(PetCategory.BIRD), true),
    MOLT("깃털·털갈이", Set.of(PetCategory.BIRD), false),
    WATER_QUALITY("어항 수질 관리", Set.of(PetCategory.FISH), true),
    WATER_TEMP("수온", Set.of(PetCategory.FISH), false),
    WATER_CHANGE("환수 기록", Set.of(PetCategory.FISH), false),
    FILTER_CLEANING("여과기 청소", Set.of(PetCategory.FISH), false);

    private final String label;
    private final Set<PetCategory> categories;
    private final boolean userDefined;

    SpecialItem(String label, Set<PetCategory> categories, boolean userDefined) {
        this.label = label;
        this.categories = categories;
        this.userDefined = userDefined;
    }

    public String label() {
        return label;
    }

    public boolean userDefined() {
        return userDefined;
    }

    public boolean appliesTo(Pet pet) {
        return categories.contains(pet.getCategory());
    }

    public static List<SpecialItem> forPet(Pet pet) {
        return Arrays.stream(values()).filter(item -> item.appliesTo(pet)).toList();
    }
}
