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
import com.goorm.ticker.restaurant.entity.ReservationPolicy;
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
		ReservationSlot slot = reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
				request.getReservationTime(), request.getRestaurantId())
			.orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_SLOT_NOT_FOUND));

		// 예약이 가능한지 확인
		if (slot.getAvailablePartySize() < request.getPartySize()) {
			throw new CustomException(ErrorCode.PARTY_SIZE_EXCEEDED);
		}

		User user = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		ReservationStatus initialStatus = switch (restaurant.getReservationPolicy()) {
			case INSTANT_CONFIRMATION -> ReservationStatus.CONFIRMED;
			case MANUAL_CONFIRMATION -> ReservationStatus.PENDING;
		};

		Reservation reservation = Reservation.of(
			restaurant,
			slot,
			request.getReservationDate(),
			user,
			request.getPartySize(),
			initialStatus
		);

		if (restaurant.getReservationPolicy() == ReservationPolicy.INSTANT_CONFIRMATION) {
			slot.updateAvailablePartySize(slot.getAvailablePartySize() - request.getPartySize());
		}

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

	@Transactional
	public ReservationCreateResponse updateReservation(Long reservationId, String status) {
		// 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

		// 예약 상태 확인
		ReservationStatus newStatus;
		try {
			newStatus = ReservationStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
		}

		if (reservation.getStatus() == newStatus || reservation.getStatus() == ReservationStatus.ENTERED
			|| reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw new CustomException(ErrorCode.RESERVATION_ALREADY_UPDATED);
		}

		// 예약 상태 업데이트
		switch (newStatus) {
			case CANCELLED -> handleCancellation(reservation);
			case ENTERED -> handleEntry(reservation);
			case CONFIRMED -> handleConfirmation(reservation);
			default -> throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
		}

		return ReservationCreateResponse.builder()
			.reservationId(reservation.getReservationId())
			.restaurantName(reservation.getRestaurant().getRestaurantName())
			.username(reservation.getUser().getName())
			.reservationDate(reservation.getReservationDate())
			.reservationTime(reservation.getReservationSlot().getSlotTime())
			.partySize(reservation.getPartySize())
			.status(reservation.getStatus())
			.build();
	}

	private void handleCancellation(Reservation reservation) {
		if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
			ReservationSlot slot = reservation.getReservationSlot();
			slot.updateAvailablePartySize(slot.getAvailablePartySize() + reservation.getPartySize());
		}
		reservation.cancelReservation();
	}

	private void handleEntry(Reservation reservation) {
		reservation.enterReservation();
	}

	private void handleConfirmation(Reservation reservation) {
		ReservationSlot slot = reservation.getReservationSlot();
		if (slot.getAvailablePartySize() < reservation.getPartySize()) {
			throw new CustomException(ErrorCode.PARTY_SIZE_EXCEEDED);
		}
		reservation.confirmReservation();
		slot.updateAvailablePartySize(slot.getAvailablePartySize() - reservation.getPartySize());
	}
}
