package com.goorm.ticker.reservation.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationCreateRequest {
	@NotNull(message = "사용자 ID는 필수입니다.")
	private Long userId;

	@NotNull(message = "음식점 ID는 필수입니다.")
	private Long restaurantId;

	@NotNull(message = "예약 날짜는 필수입니다.")
	private LocalDate reservationDate;

	@NotNull(message = "예약 시간대는 필수입니다.")
	private LocalTime reservationTime;

	@NotNull
	@Min(value = 1, message = "예약 인원은 최소 1명 이상이어야 합니다.")
	private int partySize;

	public static ReservationCreateRequest of(Long userId, Long restaurantId, LocalTime reservationTime,
		LocalDate reservationDate,
		int partySize) {
		ReservationCreateRequest request = new ReservationCreateRequest();
		request.userId = userId;
		request.restaurantId = restaurantId;
		request.reservationDate = reservationDate;
		request.reservationTime = reservationTime;
		request.partySize = partySize;
		return request;
	}
}