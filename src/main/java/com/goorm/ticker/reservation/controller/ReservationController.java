package com.goorm.ticker.reservation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "예약 API", description = "예약 생성 및 관리 API")
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
	private final ReservationService reservationService;

	@Operation(summary = "예약 생성", description = "새로운 예약을 생성합니다.")
	@PostMapping
	public ResponseEntity<ReservationCreateResponse> createReservation(
		@Valid @RequestBody ReservationCreateRequest request,
		@Parameter(hidden = true) HttpSession httpSession) {
		Long userId = (Long)httpSession.getAttribute("user");

		if (userId == null) {
			throw new CustomException(ErrorCode.SESSION_EXPIRED);
		}

		ReservationCreateResponse response = reservationService.reserve(request, userId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "예약 상태 변경", description = "예약을 취소하거나 상태를 변경합니다.")
	@PatchMapping("/{reservationId}")
	public ResponseEntity<ReservationCreateResponse> updateReservation(
		@Parameter(description = "예약 ID", required = true) @PathVariable Long reservationId,
		@Parameter(description = "변경할 상태값 (예: CANCELLED, CONFIRMED 등)") @RequestBody Map<String, String> updateRequest,
		@Parameter(hidden = true) HttpSession httpSession) {
		String status = updateRequest.get("status");

		Long userId = (Long)httpSession.getAttribute("user");
		if (userId == null) {
			throw new CustomException(ErrorCode.SESSION_EXPIRED);
		}
		ReservationCreateResponse response = reservationService.updateReservation(reservationId, status, userId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "예약 목록 조회", description = "현재 로그인한 사용자의 예약 목록을 상태별로 조회합니다.")
	@GetMapping
	public ResponseEntity<List<ReservationCreateResponse>> getReservations(
		@Parameter(description = "조회할 예약 상태 (예: CONFIRMED, CANCELLED, PENDING)")
		@RequestParam(required = false) String status,
		@Parameter(hidden = true) HttpSession httpSession) {

		Long userId = (Long)httpSession.getAttribute("user");
		List<ReservationCreateResponse> reservations = reservationService.getReservationsByUserAndStatus(userId,
			status);
		return ResponseEntity.ok(reservations);
	}
}
