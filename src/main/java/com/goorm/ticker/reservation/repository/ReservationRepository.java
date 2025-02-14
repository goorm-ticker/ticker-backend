package com.goorm.ticker.reservation.repository;

import com.goorm.ticker.reservation.Entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.ticker.reservation.Entity.Reservation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT r FROM Reservation r WHERE r.reservationDateTime < :threshold AND r.status <> :status")
    List<Reservation> findByReservationDateTimeBeforeAndStatusNot(
            @Param("threshold") LocalDateTime threshold,
            @Param("status") ReservationStatus status
    );
}
