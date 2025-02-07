package com.goorm.ticker.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Users
	DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST, "중복된 유저이름입니다."),
	NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
	LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
	SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다."),

	// Reservation
	RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 음식점입니다."),
	RESERVATION_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약 시간대입니다."),
	PARTY_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "해당 시간대에 예약 가능한 인원이 초과되었습니다."),
	RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."),
	RESERVATION_ALREADY_UPDATED(HttpStatus.BAD_REQUEST, "이미 처리한 예약입니다."),
	INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "존재하지 않는 예약 상태입니다.");

	private final HttpStatus status;
	private final String message;
}
