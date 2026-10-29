package com.petroom.tank;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TankFishRepository extends JpaRepository<TankFish, Long> {

    List<TankFish> findAllByPetIdOrderByIdAsc(Long petId);

    Optional<TankFish> findByIdAndPetId(Long id, Long petId);
}
