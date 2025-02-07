package com.goorm.ticker.restaurant.repository;

import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.ticker.restaurant.entity.ReservationSlot;

public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {
	Optional<ReservationSlot> findBySlotTimeAndRestaurantId(LocalTime slotTime, Long restaurantId);
}
