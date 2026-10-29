package com.petroom.pet;

public enum PetSubCategory {
    DOG(PetCategory.MAMMAL),
    CAT(PetCategory.MAMMAL),
    FINCH(PetCategory.BIRD),
    PARROT(PetCategory.BIRD);

    private final PetCategory category;

    PetSubCategory(PetCategory category) {
        this.category = category;
    }

    public PetCategory category() {
        return category;
    }
}
