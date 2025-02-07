package com.goorm.ticker.reservation.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.goorm.ticker.reservation.Entity.ReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationCreateResponse {
	private Long reservationId;
	private String restaurantName;
	private String username;
	private LocalDate reservationDate;
	private LocalTime reservationTime;
	private int partySize;
	private ReservationStatus status;
}
