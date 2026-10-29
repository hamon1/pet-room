package com.petroom.pet;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 반려동물에 딸린 자원(기록, 물고기 등)에 접근하기 전에 소유권을 확인합니다.
 * 다른 사용자의 반려동물은 존재 여부를 드러내지 않도록 항상 PET_NOT_FOUND로 응답합니다.
 */
@Component
public class OwnedPets {

    private final PetRepository petRepository;

    public OwnedPets(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    public Pet load(Long ownerId, Long petId) {
        return petRepository.findByIdAndOwnerId(petId, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    }

    /** 여러 마리를 한 번에 확인합니다. 하나라도 내 반려동물이 아니면 실패합니다. */
    public Map<Long, Pet> loadAll(Long ownerId, Collection<Long> petIds) {
        List<Pet> pets = petRepository.findAllByOwnerIdAndIdIn(ownerId, petIds);
        if (pets.size() != petIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
        return pets.stream().collect(Collectors.toMap(Pet::getId, Function.identity()));
    }
}
