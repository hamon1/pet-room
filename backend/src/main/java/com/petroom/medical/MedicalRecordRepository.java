package com.petroom.medical;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findAllByPetIdOrderByRecordDateDescIdDesc(Long petId);

    Optional<MedicalRecord> findByIdAndPetId(Long id, Long petId);
}
