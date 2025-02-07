package com.goorm.ticker.restaurant.repository;

import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goorm.ticker.restaurant.entity.ReservationSlot;

public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {
	@Query("SELECT r FROM ReservationSlot r WHERE r.slotTime = :slotTime AND r.restaurant.restaurantId = :restaurantId")
	Optional<ReservationSlot> findBySlotTimeAndRestaurantId(@Param("slotTime") LocalTime slotTime,
		@Param("restaurantId") Long restaurantId);
}
