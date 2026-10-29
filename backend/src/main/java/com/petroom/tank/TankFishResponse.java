package com.petroom.tank;

public record TankFishResponse(Long id, String name, String species) {

    public static TankFishResponse from(TankFish f) {
        return new TankFishResponse(f.getId(), f.getName(), f.getSpecies());
    }
}
