package com.goorm.ticker.fixture;

import java.time.LocalTime;

import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;

public enum ReservationSlotFixture {
	SLOT_FIXTURE_1(
		LocalTime.of(12, 0, 0),
		20,
		20
	),
	SLOT_FIXTURE_2(
		LocalTime.of(13, 0, 0),
		28,
		30
	),
	SLOT_FIXTURE_3(
		LocalTime.of(18, 0, 0),
		15,
		20
	),
	SLOT_FIXTURE_4(
		LocalTime.of(17, 0, 0),
		0,
		15
	);

	private final LocalTime slotTime;
	private final Integer availablePartySize;
	private final Integer maxPartySize;

	ReservationSlotFixture(LocalTime slotTime, Integer availablePartySize, Integer maxPartySize) {
		this.slotTime = slotTime;
		this.availablePartySize = availablePartySize;
		this.maxPartySize = maxPartySize;
	}

	public ReservationSlot createSlot(Restaurant restaurant) {
		return ReservationSlot.of(restaurant, slotTime, availablePartySize, maxPartySize);
	}
}
