package com.goorm.ticker.reservation.repository;

import com.goorm.ticker.reservation.Entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.goorm.ticker.reservation.Entity.Reservation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.goorm.ticker.user.entity.User;

import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByStatus(ReservationStatus status);

    List<Reservation> findByUser(User user);

    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    @Query("SELECT r FROM Reservation r " + "WHERE r.status = 'PENDING' " + "AND r.reservationSlot.slotTime < :threshold")
    List<Reservation> findPendingReservationsPastSlotTime(@Param("threshold") LocalTime threshold);
}
