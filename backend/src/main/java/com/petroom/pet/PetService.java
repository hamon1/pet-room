package com.petroom.pet;

import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {

    private static final BigDecimal DAYS_PER_YEAR = BigDecimal.valueOf(365);

    private final PetRepository petRepository;
    private final OwnedPets ownedPets;
    private final Clock clock;

    public PetService(PetRepository petRepository, OwnedPets ownedPets, Clock clock) {
        this.petRepository = petRepository;
        this.ownedPets = ownedPets;
        this.clock = clock;
    }

    @Transactional
    public PetResponse create(Long ownerId, PetRequest request) {
        Pet pet = petRepository.save(new Pet(ownerId, toProfile(request)));
        return PetResponse.from(pet);
    }

    @Transactional(readOnly = true)
    public List<PetResponse> list(Long ownerId) {
        return petRepository.findAllByOwnerIdOrderByIdAsc(ownerId).stream()
                .map(PetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetResponse get(Long ownerId, Long petId) {
        return PetResponse.from(findOwned(ownerId, petId));
    }

    @Transactional
    public PetResponse update(Long ownerId, Long petId, PetRequest request) {
        Pet pet = findOwned(ownerId, petId);
        pet.update(toProfile(request));
        petRepository.flush();
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(Long ownerId, Long petId) {
        petRepository.delete(findOwned(ownerId, petId));
    }

    private Pet findOwned(Long ownerId, Long petId) {
        return ownedPets.load(ownerId, petId);
    }

    PetProfile toProfile(PetRequest request) {
        if (request.birthDate() != null && request.ageYears() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출생일과 나이 중 하나만 입력해 주세요.");
        }
        LocalDate birthDate = request.birthDate();
        boolean estimated = false;
        if (request.ageYears() != null) {
            long days = request.ageYears().multiply(DAYS_PER_YEAR).setScale(0, RoundingMode.HALF_UP).longValue();
            birthDate = LocalDate.now(clock).minusDays(days);
            estimated = true;
        }
        return new PetProfile(
                request.name().strip(),
                request.category(),
                request.subCategory(),
                request.managementType(),
                blankToNull(request.species()),
                birthDate,
                estimated,
                blankToNull(request.traits()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
