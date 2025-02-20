package com.goorm.ticker.reservation.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationCreateRequest {

	@NotNull(message = "음식점 ID는 필수입니다.")
	private Long restaurantId;

	@NotNull(message = "예약 날짜는 필수입니다.")
	@Schema(example = "2025-03-28")
	private LocalDate reservationDate;

	@NotNull(message = "예약 시간대는 필수입니다.")
	@JsonFormat(pattern = "HH:mm:ss")  // 직렬화 패턴 설정
	@Schema(description = "예약 시간 (HH:mm:ss 형식)", example = "12:00:00")
	private LocalTime reservationTime;

	@NotNull
	@Min(value = 1, message = "예약 인원은 최소 1명 이상이어야 합니다.")
	@Schema(description = "예약 인원 수", example = "2")
	private int partySize;

	public static ReservationCreateRequest of(Long userId, Long restaurantId, LocalTime reservationTime,
		LocalDate reservationDate,
		int partySize) {
		ReservationCreateRequest request = new ReservationCreateRequest();
		request.restaurantId = restaurantId;
		request.reservationDate = reservationDate;
		request.reservationTime = reservationTime;
		request.partySize = partySize;
		return request;
	}
}