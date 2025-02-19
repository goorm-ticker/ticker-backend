package com.goorm.ticker.restaurant.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.ticker.restaurant.dto.ReservationSlotDto;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.ReservationSlotRepository;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationSlotService {
	private final ReservationSlotRepository reservationSlotRepository;
	private final RestaurantRepository restaurantRepository;

	/**
	 * 특정 식당의 예약 가능한 시간 조회 (slotId 포함)
	 */
	@Transactional(readOnly = true)
	public List<ReservationSlotDto> getRestaurantSlots(Long restaurantId) {
		return reservationSlotRepository.findByRestaurantId(restaurantId)
			.stream()
			.map(ReservationSlotDto::fromEntity)
			.collect(Collectors.toList());
	}

	@Transactional
	public void initializeTestData() {
		// 모든 식당 조회
		List<Restaurant> restaurants = restaurantRepository.findAll();

		// 예약 가능한 시간대 설정
		List<LocalTime> availableTimes = getDefaultAvailableTimes();

		// 모든 식당에 대해 예약 슬롯 추가
		List<ReservationSlot> slots = new ArrayList<>();
		for (Restaurant restaurant : restaurants) {
			for (LocalTime time : availableTimes) {
				slots.add(ReservationSlot.of(restaurant, time, 20, 20));
			}
		}

		reservationSlotRepository.saveAll(slots);
	}

	/**
	 * 기본 예약 가능한 시간대 반환
	 */
	private List<LocalTime> getDefaultAvailableTimes() {
		return List.of(
			LocalTime.of(12, 0),
			LocalTime.of(13, 0),
			LocalTime.of(17, 0),
			LocalTime.of(18, 0)
		);
	}
}
