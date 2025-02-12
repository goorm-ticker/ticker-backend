package com.goorm.ticker.waitlist.repository;

import com.goorm.ticker.map.dto.MapUpdateDto;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaitListRepository extends JpaRepository<WaitList, Long> {

    @Query("SELECT MAX(w.waitingNumber) FROM WaitList w WHERE w.restaurant.id = :restaurantId")
    Integer findMaxWaitingNumberByRestaurantId(@Param("restaurantId") Long restaurantId);

    Optional<WaitList> findByUser_IdAndStatus(Long userId, Status status);
    boolean existsByRestaurant_RestaurantIdAndUser_IdAndStatus(Long restaurantId, Long userId, Status status);
    long countByRestaurant_RestaurantIdAndWaitingNumberLessThanAndStatus(Long restaurantId, int waitingNumber, Status status);
    Optional<WaitList> findByRestaurant_RestaurantIdAndUser_IdAndStatus(Long restaurantId, Long userId, Status status);
    @Query("SELECT COUNT(w) FROM WaitList w WHERE w.restaurant.id = :restaurantId AND w.status = 'WAITING'")
    long countTotalWaitingByRestaurantId(@Param("restaurantId") Long restaurantId);

    /*
    List<WaitList> findByRestaurant_RestaurantIdAndWaitingNumberGreaterThan(Long restaurantId, int waitingNumber);
    List<WaitList> findByRestaurant_RestaurantIdAndStatusOrderByWaitingNumberAsc(Long restaurantId, Status status);
    */


    @Query("""
    SELECT new com.goorm.ticker.map.dto.MapUpdateDto(
        r.restaurantId,
        r.restaurantName,
        r.x,
        r.y,
        COUNT(w)
    )
    FROM Restaurant r 
    LEFT JOIN WaitList w ON r = w.restaurant AND w.status = 'WAITING' 
    WHERE r.restaurantId IN :restaurantIds 
    GROUP BY r
""")
    List<MapUpdateDto> findRestaurantsWithWaiting(@Param("restaurantIds") List<Long> restaurantIds);


}
