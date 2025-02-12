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


	public static Restaurant of(Long restaurantId,String restaurantName, String x, String y, Integer maxWaiting) {
		return Restaurant.builder()
				.restaurantId(restaurantId)
				.restaurantName(restaurantName)
				.x(x)
				.y(y)
				.maxWaiting(maxWaiting)
				.build();
	}
}
