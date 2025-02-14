package com.goorm.ticker.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ReservationPolicy {
	INSTANT_CONFIRMATION, MANUAL_CONFIRMATION
}
