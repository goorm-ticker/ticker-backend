package com.goorm.ticker.reservation.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.service.ReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
	private final ReservationService reservationService;

	@PostMapping
	public ResponseEntity<ReservationCreateResponse> createReservation(
		@Valid @RequestBody ReservationCreateRequest request) {
		ReservationCreateResponse response = reservationService.reserve(request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{reservationId}")
	public ResponseEntity<ReservationCreateResponse> updateReservation(
		@PathVariable Long reservationId,
		@RequestBody Map<String, String> updateRequest) {
		String status = updateRequest.get("status");
		ReservationCreateResponse response = reservationService.updateReservation(reservationId, status);
		return ResponseEntity.ok(response);
	}
}
