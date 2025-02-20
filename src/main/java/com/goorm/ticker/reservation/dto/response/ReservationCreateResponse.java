package com.goorm.ticker.reservation.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.goorm.ticker.reservation.Entity.ReservationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationCreateResponse {
	private Long reservationId;
	private String restaurantName;
	private String username;
	@Schema(example = "2025-03-28")
	private LocalDate reservationDate;
	@Schema(description = "예약 시간 (HH:mm:ss 형식)", example = "12:00:00")
	private LocalTime reservationTime;
	private int partySize;
	private ReservationStatus status;
}
