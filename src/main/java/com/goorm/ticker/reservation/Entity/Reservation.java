package com.goorm.ticker.reservation.Entity;

import java.time.LocalDate;

import com.goorm.ticker.common.entity.BaseTimeEntity;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reservations")
@NoArgsConstructor
public class Reservation extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reservationId;

	// @ManyToOne(fetch = FetchType.LAZY)
	// @JoinColumn(name = "user_id", nullable = false)
	// private Users users;

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
	private ReservationStatus status = ReservationStatus.WAITING;
}
