package com.petroom.record;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Optional<HealthRecord> findByPetIdAndRecordDate(Long petId, LocalDate recordDate);

    List<HealthRecord> findAllByPetIdAndRecordDateBetweenOrderByRecordDateDesc(Long petId, LocalDate from, LocalDate to);

    List<HealthRecord> findAllByPetIdInAndRecordDate(Collection<Long> petIds, LocalDate recordDate);

    List<HealthRecord> findAllByPetIdInAndRecordDateBetween(Collection<Long> petIds, LocalDate from, LocalDate to);
}
