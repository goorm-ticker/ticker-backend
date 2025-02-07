package com.goorm.ticker.restaurant.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	@OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReservationSlot> reservationSlots = new ArrayList<>();

	@Builder
	public Restaurant(Long restaurantId, String restaurantName, String x, String y, Integer maxWaiting){
		this.restaurantId = restaurantId;
		this.restaurantName  = restaurantName;
		this.x = x;
		this.y = y;
		this.maxWaiting = maxWaiting;
	}



}
