package com.petroom.special;

import com.petroom.global.config.Today;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.pet.OwnedPets;
import com.petroom.pet.Pet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecialRecordService {

    private final SpecialRecordRepository repository;
    private final OwnedPets ownedPets;
    private final Today today;

    public SpecialRecordService(SpecialRecordRepository repository, OwnedPets ownedPets, Today today) {
        this.repository = repository;
        this.ownedPets = ownedPets;
        this.today = today;
    }

    @Transactional(readOnly = true)
    public List<SpecialItemResponse> items(Long ownerId, Long petId) {
        Pet pet = ownedPets.load(ownerId, petId);
        Map<SpecialItem, SpecialRecordResponse> latest = new LinkedHashMap<>();
        for (SpecialRecord r : repository.findAllByPetIdOrderByRecordDateDescIdDesc(petId)) {
            latest.putIfAbsent(r.getItem(), SpecialRecordResponse.from(r));
        }
        return SpecialItem.forPet(pet).stream()
                .map(item -> new SpecialItemResponse(item, item.label(), item.userDefined(), latest.get(item)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialRecordResponse> list(Long ownerId, Long petId, SpecialItem item) {
        ownedPets.load(ownerId, petId);
        List<SpecialRecord> records = item == null
                ? repository.findAllByPetIdOrderByRecordDateDescIdDesc(petId)
                : repository.findAllByPetIdAndItemOrderByRecordDateDescIdDesc(petId, item);
        return records.stream().map(SpecialRecordResponse::from).toList();
    }

    @Transactional
    public SpecialRecordResponse create(Long ownerId, Long petId, SpecialRecordRequest request) {
        Pet pet = ownedPets.load(ownerId, petId);
        if (!request.item().appliesTo(pet)) {
            throw new BusinessException(ErrorCode.INVALID_SPECIAL_ITEM);
        }
        if (request.recordDate().isAfter(today.get())) {
            throw new BusinessException(ErrorCode.INVALID_RECORD, "오늘 이후 날짜에는 기록할 수 없습니다.");
        }
        String value = request.value() == null || request.value().isBlank() ? null : request.value().strip();
        SpecialRecord saved = repository.saveAndFlush(new SpecialRecord(petId, request.item(), request.recordDate(), value));
        return SpecialRecordResponse.from(saved);
    }

    @Transactional
    public void delete(Long ownerId, Long petId, Long recordId) {
        ownedPets.load(ownerId, petId);
        SpecialRecord record = repository.findByIdAndPetId(recordId, petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPECIAL_RECORD_NOT_FOUND));
        repository.delete(record);
    }
}
