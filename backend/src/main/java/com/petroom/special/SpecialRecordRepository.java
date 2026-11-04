package com.petroom.special;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialRecordRepository extends JpaRepository<SpecialRecord, Long> {

    List<SpecialRecord> findAllByPetIdOrderByRecordDateDescIdDesc(Long petId);

    List<SpecialRecord> findAllByPetIdAndItemOrderByRecordDateDescIdDesc(Long petId, SpecialItem item);

    Optional<SpecialRecord> findByIdAndPetId(Long id, Long petId);

    List<SpecialRecord> findAllByPetIdInAndRecordDateBetween(Collection<Long> petIds, LocalDate from, LocalDate to);
}
