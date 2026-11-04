package com.petroom.food;

import com.petroom.global.config.Today;
import com.petroom.global.error.BusinessException;
import com.petroom.global.error.ErrorCode;
import com.petroom.pet.OwnedPets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodService {

    private final FoodRepository repository;
    private final OwnedPets ownedPets;
    private final Today today;

    public FoodService(FoodRepository repository, OwnedPets ownedPets, Today today) {
        this.repository = repository;
        this.ownedPets = ownedPets;
        this.today = today;
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> list(Long ownerId) {
        return repository.findAllByOwnerIdOrderByExpiryDateAscIdAsc(ownerId).stream()
                .map(f -> FoodResponse.from(f, today.get()))
                .toList();
    }

    @Transactional
    public FoodResponse create(Long ownerId, FoodRequest request) {
        Food food = new Food(ownerId);
        apply(ownerId, food, request);
        return FoodResponse.from(repository.saveAndFlush(food), today.get());
    }

    @Transactional
    public FoodResponse update(Long ownerId, Long foodId, FoodRequest request) {
        Food food = find(ownerId, foodId);
        apply(ownerId, food, request);
        return FoodResponse.from(repository.saveAndFlush(food), today.get());
    }

    @Transactional
    public FoodResponse updateRemaining(Long ownerId, Long foodId, RemainingRequest request) {
        Food food = find(ownerId, foodId);
        food.setRemaining(request.remainingG());
        return FoodResponse.from(repository.saveAndFlush(food), today.get());
    }

    @Transactional
    public FoodResponse feed(Long ownerId, Long foodId) {
        Food food = find(ownerId, foodId);
        if (!food.canFeed()) {
            throw new BusinessException(ErrorCode.INVALID_FOOD, "1회 급여량과 남은 양을 먼저 입력해 주세요.");
        }
        food.feedOnce();
        return FoodResponse.from(repository.saveAndFlush(food), today.get());
    }

    @Transactional
    public void delete(Long ownerId, Long foodId) {
        repository.delete(find(ownerId, foodId));
    }

    private Food find(Long ownerId, Long foodId) {
        return repository.findByIdAndOwnerId(foodId, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_NOT_FOUND));
    }

    private void apply(Long ownerId, Food food, FoodRequest request) {
        Set<Long> petIds = request.petIds() == null ? Set.of() : new LinkedHashSet<>(request.petIds());
        if (!petIds.isEmpty()) {
            ownedPets.loadAll(ownerId, petIds);
        }
        if (request.totalG() != null && request.remainingG() != null
                && request.remainingG().compareTo(request.totalG()) > 0) {
            throw new BusinessException(ErrorCode.INVALID_FOOD, "남은 양이 전체 양보다 많을 수 없습니다.");
        }
        FoodRequest normalized = new FoodRequest(request.name(), request.expiryDate(), request.typeOrDefault(),
                request.openedDate(), request.totalG(), request.remainingG(), request.perServingG(),
                request.timesPerDay(), request.petIds());
        food.apply(normalized, petIds);
    }
}
