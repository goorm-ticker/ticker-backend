package com.goorm.ticker.fixture;

import java.time.LocalDate;

import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.user.entity.User;

public enum ReservationFixture {
	RESERVATION_FIXTURE_1(
		4,
		LocalDate.of(2025, 2, 22),
		ReservationStatus.WAITING
	),
	RESERVATION_FIXTURE_2(
		2,
		LocalDate.of(2025, 2, 22),
		ReservationStatus.CONFIRMED
	),
	RESERVATION_FIXTURE_3(
		2,
		LocalDate.of(2025, 2, 22),
		ReservationStatus.CANCELLED
	),
	RESERVATION_FIXTURE_4(
		2,
		LocalDate.of(2025, 2, 22),
		ReservationStatus.COMPLETED
	);

	private final Integer partySize;
	private final LocalDate reservationDate;
	private ReservationStatus status;

	ReservationFixture(Integer partySize, LocalDate reservationDate, ReservationStatus status) {
		this.partySize = partySize;
		this.reservationDate = reservationDate;
		this.status = status;
	}

	public Reservation createReservation(Restaurant restaurant, ReservationSlot slot,
		User user) {
		return Reservation.of(
			restaurant,
			slot,
			reservationDate,
			user,
			partySize,
			status
		);
	}
}
