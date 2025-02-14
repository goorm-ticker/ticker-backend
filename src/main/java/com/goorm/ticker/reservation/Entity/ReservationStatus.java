package com.goorm.ticker.reservation.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ReservationStatus {
	PENDING, CONFIRMED, ENTERED, CANCELLED
}
