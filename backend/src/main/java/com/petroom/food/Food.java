package com.petroom.food;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 사용자 단위 먹이 재고. 여러 마리가 같은 먹이를 먹으면 food_pets로 연결해 재고를 하나로 관리합니다. */
@Entity
@Table(name = "foods")
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodType type;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "opened_date")
    private LocalDate openedDate;

    @Column(name = "total_g", precision = 9, scale = 1)
    private BigDecimal totalG;

    @Column(name = "remaining_g", precision = 9, scale = 1)
    private BigDecimal remainingG;

    @Column(name = "per_serving_g", precision = 7, scale = 1)
    private BigDecimal perServingG;

    @Column(name = "times_per_day")
    private Integer timesPerDay;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "food_pets", joinColumns = @JoinColumn(name = "food_id"))
    @Column(name = "pet_id")
    private Set<Long> petIds = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Food() {
    }

    public Food(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void apply(FoodRequest r, Set<Long> petIds) {
        this.name = r.name().strip();
        this.type = r.type();
        this.expiryDate = r.expiryDate();
        this.openedDate = r.openedDate();
        this.remainingG = r.remainingG();
        // 처음 등록할 때 남은 양을 전체 양으로 봅니다. 남은 양이 전체보다 많아지면 전체를 늘립니다.
        if (r.totalG() != null) {
            this.totalG = r.totalG();
        } else if (this.totalG == null || (r.remainingG() != null && r.remainingG().compareTo(this.totalG) > 0)) {
            this.totalG = r.remainingG();
        }
        this.perServingG = r.perServingG();
        this.timesPerDay = r.timesPerDay();
        this.petIds.clear();
        this.petIds.addAll(petIds);
    }

    public void setRemaining(BigDecimal remaining) {
        this.remainingG = remaining;
        if (remaining != null && (totalG == null || remaining.compareTo(totalG) > 0)) {
            this.totalG = remaining;
        }
    }

    /** 한 번 급여: 1회 급여량 × 먹는 동물 수만큼 줄입니다. 0 아래로는 내려가지 않습니다. */
    public BigDecimal feedOnce() {
        BigDecimal used = perServingG.multiply(BigDecimal.valueOf(eaters()));
        BigDecimal next = remainingG.subtract(used);
        this.remainingG = next.signum() < 0 ? BigDecimal.ZERO.setScale(1) : next;
        return used;
    }

    public boolean canFeed() {
        return perServingG != null && perServingG.signum() > 0 && remainingG != null;
    }

    /** 하루 사용량 = 1회 급여량 × 하루 횟수 × 먹는 동물 수. 정보가 부족하면 null. */
    public BigDecimal dailyUse() {
        if (perServingG == null || timesPerDay == null || perServingG.signum() <= 0 || timesPerDay <= 0) {
            return null;
        }
        return perServingG.multiply(BigDecimal.valueOf((long) timesPerDay * eaters()));
    }

    /** 소진 예상일 = 오늘 + floor(남은 양 ÷ 하루 사용량). 계산할 수 없으면 null. */
    public LocalDate runoutDate(LocalDate today) {
        BigDecimal daily = dailyUse();
        if (daily == null || remainingG == null) {
            return null;
        }
        long days = remainingG.divide(daily, 0, RoundingMode.FLOOR).longValue();
        return today.plusDays(days);
    }

    private int eaters() {
        return Math.max(1, petIds.size());
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public FoodType getType() {
        return type;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDate getOpenedDate() {
        return openedDate;
    }

    public BigDecimal getTotalG() {
        return totalG;
    }

    public BigDecimal getRemainingG() {
        return remainingG;
    }

    public BigDecimal getPerServingG() {
        return perServingG;
    }

    public Integer getTimesPerDay() {
        return timesPerDay;
    }

    public Set<Long> getPetIds() {
        return petIds;
    }
}
