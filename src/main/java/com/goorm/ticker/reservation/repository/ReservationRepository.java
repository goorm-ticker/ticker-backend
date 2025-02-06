package com.goorm.ticker.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.ticker.reservation.Entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
