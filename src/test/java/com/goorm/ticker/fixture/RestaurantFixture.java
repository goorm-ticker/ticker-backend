package com.goorm.ticker.fixture;

import com.goorm.ticker.restaurant.entity.Restaurant;

public enum RestaurantFixture {
	RESTAURANT_FIXTURE_1(
			1L,
		"Test Restaurant 1",
		"37.5665",
		"126.9780",
		50),
	RESTAURANT_FIXTURE_2(
			2L,
		"Test Restaurant 2",
		"37.5675",
		"126.9790",
		30);
	private final Long restaurantId;

	private final String name;
	private final String x;
	private final String y;
	private final Integer maxWaiting;

	RestaurantFixture(Long restaurantId,String name, String x, String y, Integer maxWaiting) {
		this.restaurantId = restaurantId;
		this.name = name;
		this.x = x;
		this.y = y;
		this.maxWaiting = maxWaiting;
	}

	public Restaurant createRestaurant() {
		return Restaurant.of(restaurantId,name, x, y, maxWaiting);
	}

}
