package com.goorm.ticker.reservation.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.service.ReservationService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
	private final ReservationService reservationService;

	@PostMapping
	public ResponseEntity<ReservationCreateResponse> createReservation(
		@Valid @RequestBody ReservationCreateRequest request,
		HttpSession httpSession) {
		Long userId = (Long)httpSession.getAttribute("userId");

		if (userId == null) {
			throw new CustomException(ErrorCode.SESSION_EXPIRED);
		}

		ReservationCreateResponse response = reservationService.reserve(request, userId);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{reservationId}")
	public ResponseEntity<ReservationCreateResponse> updateReservation(
		@PathVariable Long reservationId,
		@RequestBody Map<String, String> updateRequest,
		HttpSession httpSession) {
		String status = updateRequest.get("status");

		Long userId = (Long)httpSession.getAttribute("userId");
		if (userId == null) {
			throw new CustomException(ErrorCode.SESSION_EXPIRED);
		}
		ReservationCreateResponse response = reservationService.updateReservation(reservationId, status, userId);
		return ResponseEntity.ok(response);
	}
}
