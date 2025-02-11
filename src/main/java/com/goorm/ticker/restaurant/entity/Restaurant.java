package com.goorm.ticker.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "restaurants")
public class Restaurant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "restaurant_id")
	private Long restaurantId;

	@Column(name = "restaurant_name", nullable = false)
	private String restaurantName;

	@Column(name = "x", nullable = false)
	private String x;

	@Column(name = "y", nullable = false)
	private String y;

	@Column(name = "max_waiting", nullable = false)
	private Integer maxWaiting;

	@Column(name = "reservation_policy", nullable = false)
	private ReservationPolicy reservation_policy;

	public static Restaurant of(String restaurantName, String x, String y, Integer maxWaiting,
		ReservationPolicy reservationPolicy) {
		return Restaurant.builder()
			.restaurantName(restaurantName)
			.x(x)
			.y(y)
			.maxWaiting(maxWaiting)
			.reservation_policy(reservationPolicy)
			.build();
	}
}
