package com.goorm.ticker.fixture;

import com.goorm.ticker.restaurant.entity.ReservationPolicy;
import com.goorm.ticker.restaurant.entity.Restaurant;

public enum RestaurantFixture {
	RESTAURANT_FIXTURE_1(
		"Test Restaurant 1",
		"37.5665",
		"126.9780",
		50,
		ReservationPolicy.INSTANT_CONFIRMATION),
	RESTAURANT_FIXTURE_2(
		"Test Restaurant 2",
		"37.5675",
		"126.9790",
		30,
		ReservationPolicy.MANUAL_CONFIRMATION);

	private final String name;
	private final String x;
	private final String y;
	private final Integer maxWaiting;
	private final ReservationPolicy reservationPolicy;

	RestaurantFixture(String name, String x, String y, Integer maxWaiting,
		ReservationPolicy reservationPolicy) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.maxWaiting = maxWaiting;
		this.reservationPolicy = reservationPolicy;
	}

	public Restaurant createRestaurant() {
		return Restaurant.of(name, x, y, maxWaiting, reservationPolicy);
	}

}
