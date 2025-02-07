package com.goorm.ticker.reservation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.ReservationSlotRepository;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final RestaurantRepository restaurantRepository;
	private final ReservationSlotRepository reservationSlotRepository;
	private final UserRepository userRepository;

	@Transactional
	public ReservationCreateResponse reserve(ReservationCreateRequest request) {
		// 음식점 조회
		Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
			.orElseThrow(() -> new CustomException(ErrorCode.RESTAURANT_NOT_FOUND));

		// 예약 슬롯이 존재하는지 조회
		ReservationSlot slot = reservationSlotRepository.findBySlotTimeAndRestaurantId(
				request.getReservationTime(), request.getRestaurantId())
			.orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_SLOT_NOT_FOUND));

		// 예약이 가능한지 확인
		if (slot.getAvailablePartySize() < request.getPartySize()) {
			throw new CustomException(ErrorCode.PARTY_SIZE_EXCEEDED);
		}

		User user = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		// 예약 가능 인원 확인
		if (slot.getAvailablePartySize() < request.getPartySize()) {
			throw new CustomException(ErrorCode.PARTY_SIZE_EXCEEDED);
		}

		Reservation reservation = Reservation.of(
			restaurant,
			slot,
			request.getReservationDate(),
			user,
			request.getPartySize(),
			ReservationStatus.WAITING
		);

		reservationRepository.save(reservation);

		return ReservationCreateResponse.builder()
			.reservationId(reservation.getReservationId())
			.restaurantName(restaurant.getRestaurantName())
			.username(user.getName())
			.reservationDate(reservation.getReservationDate())
			.reservationTime(slot.getSlotTime())
			.partySize(reservation.getPartySize())
			.status(reservation.getStatus())
			.build();
	}
}
