package com.goorm.ticker.waitlist.repository;

import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WaitListRepository extends JpaRepository<WaitList, Long> {

    @Query("SELECT MAX(w.waitingNumber) FROM WaitList w WHERE w.restaurant.id = :restaurantId")
    Integer findMaxWaitingNumberByRestaurantId(@Param("restaurantId") Long restaurantId);

    Optional<WaitList> findByUser_IdAndStatus(Long userId, Status status);
    boolean existsByRestaurant_RestaurantIdAndUser_IdAndStatus(Long restaurantId, Long userId, Status status);
    long countByRestaurant_RestaurantIdAndWaitingNumberLessThanAndStatus(Long restaurantId, int waitingNumber, Status status);
    Optional<WaitList> findByRestaurant_RestaurantIdAndUser_IdAndStatus(Long restaurantId, Long userId, Status status);

    /*
    List<WaitList> findByRestaurant_RestaurantIdAndWaitingNumberGreaterThan(Long restaurantId, int waitingNumber);
    List<WaitList> findByRestaurant_RestaurantIdAndStatusOrderByWaitingNumberAsc(Long restaurantId, Status status);
    */
}
