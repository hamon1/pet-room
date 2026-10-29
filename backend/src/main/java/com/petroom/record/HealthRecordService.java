package com.petroom.record;

import com.petroom.global.config.Today;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.pet.OwnedPets;
import com.petroom.pet.Pet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthRecordService {

    /** 기간을 정하지 않고 조회하면 최근 30일을 돌려줍니다. 한 번에 최대 1년까지 조회할 수 있습니다. */
    static final int DEFAULT_DAYS = 30;
    static final int MAX_DAYS = 366;

    private final HealthRecordRepository repository;
    private final OwnedPets ownedPets;
    private final Today today;

    public HealthRecordService(HealthRecordRepository repository, OwnedPets ownedPets, Today today) {
        this.repository = repository;
        this.ownedPets = ownedPets;
        this.today = today;
    }

    @Transactional
    public HealthRecordResponse put(Long ownerId, Long petId, LocalDate date, HealthRecordRequest request) {
        Pet pet = ownedPets.load(ownerId, petId);
        HealthValues values = request.toValues();
        validate(pet, date, values);
        HealthRecord record = repository.findByPetIdAndRecordDate(petId, date)
                .orElseGet(() -> new HealthRecord(petId, date));
        record.replace(values);
        return HealthRecordResponse.from(repository.saveAndFlush(record));
    }

    @Transactional
    public List<HealthRecordResponse> batch(Long ownerId, BatchHealthRequest request) {
        LocalDate date = request.date();
        Set<Long> seen = new HashSet<>();
        for (BatchHealthRequest.Item item : request.items()) {
            if (!seen.add(item.petId())) {
                throw new BusinessException(ErrorCode.INVALID_RECORD, "같은 반려동물이 두 번 들어 있습니다.");
            }
        }
        Map<Long, Pet> pets = ownedPets.loadAll(ownerId, seen);
        Map<Long, HealthRecord> existing = new HashMap<>(repository.findAllByPetIdInAndRecordDate(seen, date).stream()
                .collect(Collectors.toMap(HealthRecord::getPetId, Function.identity())));

        List<HealthRecord> saved = request.items().stream().map(item -> {
            HealthValues values = new HealthValues(null, item.meal(), item.stool(), item.fed(), null);
            validate(pets.get(item.petId()), date, values);
            HealthRecord record = existing.computeIfAbsent(item.petId(), id -> new HealthRecord(id, date));
            record.merge(values);
            return record;
        }).toList();
        return repository.saveAllAndFlush(saved).stream().map(HealthRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HealthRecordResponse> list(Long ownerId, Long petId, LocalDate from, LocalDate to) {
        ownedPets.load(ownerId, petId);
        LocalDate end = to != null ? to : today.get();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1);
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "조회 기간은 1년 이내로 지정해 주세요.");
        }
        return repository.findAllByPetIdAndRecordDateBetweenOrderByRecordDateDesc(petId, start, end).stream()
                .map(HealthRecordResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long ownerId, Long petId, LocalDate date) {
        ownedPets.load(ownerId, petId);
        HealthRecord record = repository.findByPetIdAndRecordDate(petId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_RECORD_NOT_FOUND));
        repository.delete(record);
    }

    /** 어항은 먹이 급여만, 개체는 체중·식사·배변만 기록합니다. */
    void validate(Pet pet, LocalDate date, HealthValues v) {
        if (date.isAfter(today.get())) {
            throw new BusinessException(ErrorCode.INVALID_RECORD, "오늘 이후 날짜에는 기록할 수 없습니다.");
        }
        if (v.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_RECORD, "기록할 내용을 하나 이상 입력해 주세요.");
        }
        if (pet.isTank()) {
            if (v.weightKg() != null || v.meal() != null || v.stool() != null) {
                throw new BusinessException(ErrorCode.INVALID_RECORD, "어항은 먹이 급여와 메모만 기록할 수 있습니다.");
            }
        } else if (v.fed() != null) {
            throw new BusinessException(ErrorCode.INVALID_RECORD, "먹이 급여 기록은 어항에만 있습니다.");
        }
    }
}
