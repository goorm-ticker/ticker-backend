package com.goorm.ticker.reservation.Entity;

import java.time.LocalDate;

import com.goorm.ticker.common.entity.BaseTimeEntity;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Reservation extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reservationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "slot_id", nullable = false)
	private ReservationSlot reservationSlot;

	@Column(name = "reservation_date", nullable = false)
	private LocalDate reservationDate;

	@Column(name = "party_size", nullable = false)
	private int partySize;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ReservationStatus status;

	public static Reservation of(Restaurant restaurant, ReservationSlot reservationSlot, LocalDate reservationDate,
		User user, Integer partySize, ReservationStatus status) {
		return Reservation.builder()
			.restaurant(restaurant)
			.reservationSlot(reservationSlot)
			.reservationDate(reservationDate)
			.user(user)
			.partySize(partySize)
			.status(status)
			.build();
	}

	public void confirmReservation() {
		this.status = ReservationStatus.CONFIRMED;
	}

	public void enterReservation() {
		this.status = ReservationStatus.ENTERED;
	}

	public void cancelReservation() {
		this.status = ReservationStatus.CANCELLED;
	}

}
