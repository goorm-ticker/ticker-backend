package com.goorm.ticker.restaurant.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goorm.ticker.restaurant.entity.ReservationSlot;

import jakarta.persistence.LockModeType;

public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM ReservationSlot r WHERE r.slotTime = :slotTime AND r.restaurant.restaurantId = :restaurantId")
	Optional<ReservationSlot> findBySlotTimeAndRestaurantIdWithLock(@Param("slotTime") LocalTime slotTime,
		@Param("restaurantId") Long restaurantId);

	@Modifying
	@Query("UPDATE ReservationSlot rs SET rs.availablePartySize = rs.availablePartySize - :partySize "
		+ "WHERE rs.id = :slotId AND rs.availablePartySize >= :partySize")
	void decreaseAvailablePartySize(@Param("slotId") Long slotId, @Param("partySize") int partySize);

	@Modifying
	@Query("UPDATE ReservationSlot rs SET rs.availablePartySize = rs.availablePartySize + :partySize "
		+ "WHERE rs.id = :slotId")
	void increaseAvailablePartySize(@Param("slotId") Long slotId, @Param("partySize") int partySize);

	@Query("SELECT s FROM ReservationSlot s WHERE s.restaurant.restaurantId = :restaurantId")
	List<ReservationSlot> findByRestaurantId(@Param("restaurantId") Long restaurantId);
}
