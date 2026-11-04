package com.petroom.push;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByUserId(Long userId);

    @Query("select distinct s.userId from PushSubscription s")
    List<Long> findSubscribedUserIds();

    @Transactional
    long deleteByEndpointAndUserId(String endpoint, Long userId);

    /** 오늘 처음이면 1, 이미 보냈으면 0. 서버가 여러 대여도 한 번만 보내도록 DB 기본키로 막습니다. */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO push_deliveries (user_id, sent_date) VALUES (:userId, :date) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int markDelivered(Long userId, LocalDate date);
}
