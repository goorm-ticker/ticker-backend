package com.goorm.ticker.restaurant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goorm.ticker.restaurant.dto.ReservationSlotDto;
import com.goorm.ticker.restaurant.service.ReservationSlotService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
	private final ReservationSlotService reservationSlotService;

	@Operation(summary = "특정 식당의 예약 가능 시간 조회")
	@GetMapping("/{restaurantId}/slots")
	public ResponseEntity<List<ReservationSlotDto>> getRestaurantSlots(@PathVariable Long restaurantId) {
		List<ReservationSlotDto> availableSlots = reservationSlotService.getRestaurantSlots(restaurantId);
		return ResponseEntity.ok(availableSlots);
	}

	@Operation(summary = "테스트 데이터 초기화", description = "모든 음식점의 예약 가능한 시간을 등록합니다.")
	@PostMapping("/slots/initialize")
	public ResponseEntity<Void> initializeTestData() {
		reservationSlotService.initializeTestData();
		return ResponseEntity.ok().build();
	}
}
