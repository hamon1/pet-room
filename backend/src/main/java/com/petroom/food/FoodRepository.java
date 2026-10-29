package com.petroom.food;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> {

    List<Food> findAllByOwnerIdOrderByExpiryDateAscIdAsc(Long ownerId);

    Optional<Food> findByIdAndOwnerId(Long id, Long ownerId);
}
