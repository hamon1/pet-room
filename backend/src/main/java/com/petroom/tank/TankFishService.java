package com.petroom.tank;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.pet.OwnedPets;
import com.petroom.pet.Pet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TankFishService {

    private final TankFishRepository repository;
    private final OwnedPets ownedPets;

    public TankFishService(TankFishRepository repository, OwnedPets ownedPets) {
        this.repository = repository;
        this.ownedPets = ownedPets;
    }

    @Transactional(readOnly = true)
    public List<TankFishResponse> list(Long ownerId, Long petId) {
        loadTank(ownerId, petId);
        return repository.findAllByPetIdOrderByIdAsc(petId).stream().map(TankFishResponse::from).toList();
    }

    @Transactional
    public TankFishResponse add(Long ownerId, Long petId, TankFishRequest request) {
        Pet tank = loadTank(ownerId, petId);
        String species = request.species() == null || request.species().isBlank()
                ? tank.getSpecies()
                : request.species().strip();
        return TankFishResponse.from(repository.saveAndFlush(new TankFish(petId, request.name().strip(), species)));
    }

    @Transactional
    public void remove(Long ownerId, Long petId, Long fishId) {
        loadTank(ownerId, petId);
        TankFish fish = repository.findByIdAndPetId(fishId, petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FISH_NOT_FOUND));
        repository.delete(fish);
    }

    private Pet loadTank(Long ownerId, Long petId) {
        Pet pet = ownedPets.load(ownerId, petId);
        if (!pet.isTank()) {
            throw new BusinessException(ErrorCode.NOT_A_TANK);
        }
        return pet;
    }
}
