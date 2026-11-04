package com.petroom.medical;

import com.petroom.global.config.Today;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.pet.OwnedPets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository repository;
    private final OwnedPets ownedPets;
    private final Today today;

    public MedicalRecordService(MedicalRecordRepository repository, OwnedPets ownedPets, Today today) {
        this.repository = repository;
        this.ownedPets = ownedPets;
        this.today = today;
    }

    @Transactional
    public MedicalRecordResponse create(Long ownerId, Long petId, MedicalRecordRequest request) {
        ownedPets.load(ownerId, petId);
        MedicalRecord record = new MedicalRecord(petId);
        apply(record, request);
        return MedicalRecordResponse.from(repository.saveAndFlush(record));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> list(Long ownerId, Long petId) {
        ownedPets.load(ownerId, petId);
        return repository.findAllByPetIdOrderByRecordDateDescIdDesc(petId).stream()
                .map(MedicalRecordResponse::from)
                .toList();
    }

    @Transactional
    public MedicalRecordResponse update(Long ownerId, Long petId, Long recordId, MedicalRecordRequest request) {
        MedicalRecord record = find(ownerId, petId, recordId);
        apply(record, request);
        return MedicalRecordResponse.from(repository.saveAndFlush(record));
    }

    @Transactional
    public void delete(Long ownerId, Long petId, Long recordId) {
        repository.delete(find(ownerId, petId, recordId));
    }

    private MedicalRecord find(Long ownerId, Long petId, Long recordId) {
        ownedPets.load(ownerId, petId);
        return repository.findByIdAndPetId(recordId, petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
    }

    private void apply(MedicalRecord record, MedicalRecordRequest r) {
        if (r.recordDate().isAfter(today.get())) {
            throw new BusinessException(ErrorCode.INVALID_RECORD, "진료·투약한 날짜는 오늘 이후일 수 없습니다. 예정일은 '다음 예정일'에 입력해 주세요.");
        }
        if (r.nextDate() != null && !r.nextDate().isAfter(r.recordDate())) {
            throw new BusinessException(ErrorCode.INVALID_RECORD, "다음 예정일은 기록 날짜 이후여야 합니다.");
        }
        String hospital = r.hospital() == null || r.hospital().isBlank() ? null : r.hospital().strip();
        record.apply(r.kind(), r.recordDate(), hospital, r.content().strip(), r.nextDate());
    }
}
