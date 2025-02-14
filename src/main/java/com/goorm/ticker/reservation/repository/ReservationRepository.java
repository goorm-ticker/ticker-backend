package com.goorm.ticker.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.user.entity.User;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	long countByStatus(ReservationStatus status);

	List<Reservation> findByUser(User user);

	List<Reservation> findByUserAndStatus(User user, ReservationStatus status);
}
