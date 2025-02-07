package com.goorm.ticker.restaurant.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "reservation_slots")
public class ReservationSlot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "slot_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(name = "slot_time", nullable = false)
	private LocalTime slotTime;

	@Column(name = "max_party_size", nullable = false)
	private int maxPartySize;

	@Column(name = "available_party_size", nullable = false)
	private int availablePartySize;

	public static ReservationSlot of(Restaurant restaurant, LocalTime slotTime,
		Integer availablePartySize, Integer maxPartySize) {
		return ReservationSlot.builder()
			.restaurant(restaurant)
			.slotTime(slotTime)
			.availablePartySize(availablePartySize)
			.maxPartySize(maxPartySize)
			.build();
	}

	public void updateAvailablePartySize(int availablePartySize) {
		this.availablePartySize = availablePartySize;
	}
}
