package com.petroom.medical;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findAllByPetIdOrderByRecordDateDescIdDesc(Long petId);

    Optional<MedicalRecord> findByIdAndPetId(Long id, Long petId);

    List<MedicalRecord> findAllByPetIdInAndNextDateBetween(Collection<Long> petIds, LocalDate from, LocalDate to);

    List<MedicalRecord> findAllByPetIdInAndRecordDateBetween(Collection<Long> petIds, LocalDate from, LocalDate to);
}
