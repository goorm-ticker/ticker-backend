package com.goorm.ticker.reservation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.notification.entity.Notification;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.notification.service.FCMService;
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
import com.goorm.ticker.waitlist.service.CompleteWaitingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final RestaurantRepository restaurantRepository;
	private final ReservationSlotRepository reservationSlotRepository;
	private final UserRepository userRepository;
	private final ReservationStatusPublisher reservationStatusPublisher;

	private final FCMService fcmService;
	private final NotificationRepository notificationRepository;
	private final CompleteWaitingService completeWaitingService;

	@Transactional
	public ReservationCreateResponse reserve(ReservationCreateRequest request, Long userId) {

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

		User user = userRepository.findById(userId)
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

		reservation = reservationRepository.save(reservation);
		reservationRepository.flush(); // 저장 후 즉시 반영

		if (reservation.getReservationId() == null) {
			log.warn("예약 저장 후 reservationId가 null입니다: {}", reservation);
			throw new CustomException(ErrorCode.RESERVATION_NOT_FOUND);
		}

		log.info("예약 성공: reservationId={}", reservation.getReservationId());

		if (restaurant.getReservationPolicy() == ReservationPolicy.INSTANT_CONFIRMATION) {
			slot.updateAvailablePartySize(slot.getAvailablePartySize() - request.getPartySize());
			reservationSlotRepository.saveAndFlush(slot);
			// 즉시 확정 예약 시 CONFIRM 알림 전송
			reservationStatusPublisher.publishReservationStatus(reservation.getReservationId(), "CONFIRMED");
			// 원자적으로 가용 인원 감소
			// reservationSlotRepository.decreaseAvailablePartySize(slot.getId(), request.getPartySize());
		}

		log.info("[O] 예약 성공 - 유저 ID: {} | 예약 ID: {} | 남은 예약 가능 인원: {}",
			reservation.getUser().getId(), reservation.getReservationId(),
			reservation.getReservationSlot().getAvailablePartySize());

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
	public ReservationCreateResponse updateReservation(Long reservationId, String status, Long userId) {
		log.info("예약 상태 업데이트 요청: reservationId={}, status={}", reservationId, status);

		// 예약 조회
		Reservation reservation = reservationRepository.findById(reservationId)
			.orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

		if (!reservation.getUser().getId().equals(userId)) {
			throw new CustomException(ErrorCode.FORBIDDEN_RESERVATION_ACCESS);
		}

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
			case CANCELLED -> {
				handleCancellation(reservation);
				log.info("예약 취소 처리 완료: reservationId={}", reservationId);
				reservationStatusPublisher.publishReservationStatus(reservationId, "CANCELLED");
				log.info("예약 취소 상태 변경 이벤트 전송 완료: reservationId={}", reservationId);
			}
			case ENTERED -> handleEntry(reservation);
			case CONFIRMED -> {
				handleConfirmation(reservation);
				log.info("예약 확정 처리 완료: reservationId={}", reservationId);
				reservationStatusPublisher.publishReservationStatus(reservationId, "CONFIRMED");
				log.info("예약 확정 상태 변경 이벤트 전송 완료: reservationId={}", reservationId);
			}
			default -> throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
		}

		reservation.updateLastNotificationStatus(newStatus.name());
		reservationRepository.saveAndFlush(reservation);

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
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw new CustomException(ErrorCode.RESERVATION_ALREADY_UPDATED);
		}

		if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
			// 원자적으로 가용 인원 증가
			// reservationSlotRepository.increaseAvailablePartySize(reservation.getReservationSlot().getId(),
			// 	reservation.getPartySize());
			ReservationSlot slot = reservation.getReservationSlot();
			slot.updateAvailablePartySize(slot.getAvailablePartySize() - reservation.getPartySize());
			reservationSlotRepository.saveAndFlush(slot);
		}
		reservation.cancelReservation();

		log.info("[O] 취소 성공 - 유저 ID: {} | 예약 ID: {} | 남은 예약 가능 인원: {}",
			reservation.getUser().getId(), reservation.getReservationId(),
			reservation.getReservationSlot().getAvailablePartySize());
	}

	private void handleEntry(Reservation reservation) {
		log.info("예약 상태 업데이트 - ENTERED: reservationId={}", reservation.getReservationId());

		reservation.enterReservation();

		completeWaitingService.completeWaiting(reservation.getUser().getId());

		reservationStatusPublisher.publishReservationStatus(reservation.getReservationId(), "ENTERED");
	}

	private void handleConfirmation(Reservation reservation) {
		ReservationSlot slot = reservation.getReservationSlot();
		if (slot.getAvailablePartySize() < reservation.getPartySize()) {
			throw new CustomException(ErrorCode.PARTY_SIZE_EXCEEDED);
		}
		reservation.confirmReservation();
		slot.updateAvailablePartySize(slot.getAvailablePartySize() - reservation.getPartySize());
	}

	public boolean isStatusAlreadyUpdated(Reservation reservation, String newStatus) {
		if (reservation == null || reservation.getReservationId() == null) {
			log.warn("isStatusAlreadyUpdated() 호출 시 예약 정보가 없음: reservation={}", reservation);
			return false;
		}
		return reservation.getStatus().name().equals(newStatus);
	}

	public boolean existsById(Long reservationId) {
		return reservationRepository.existsById(reservationId);
	}

	public Reservation getReservationById(Long reservationId) {
		return reservationRepository.findById(reservationId)
			.orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

	}

	@Transactional
	public void sendNotificationIfNeeded(Reservation reservation, String newStatus) {
		log.info("sendNotificationIfNeeded 실행: reservationId={}, newStatus={}", reservation.getReservationId(),
			newStatus);

		if (reservation.isNotificationAlreadySent(newStatus)) {
			log.info("중복 알림 방지: reservationId={}, status={}", reservation.getReservationId(), newStatus);
			return;
		}

		reservation.updateLastNotificationStatus(newStatus);
		reservationRepository.save(reservation);

		String notificationMessage = switch (newStatus) {
			case "CONFIRMED" -> "예약이 확정되었습니다.";
			case "CANCELLED" -> "예약이 취소되었습니다.";
			default -> "예약 상태가 변경되었습니다.";
		};

		log.info("알림 전송: reservationId={}, message={}", reservation.getReservationId(), notificationMessage);
		fcmService.sentNotification("general", "예약 알림", notificationMessage);

		Notification notification = Notification.createNotification(
			reservation.getUser(),
			notificationMessage,
			newStatus.equals("CONFIRMED") ? NotificationType.RESERVATION_CONFIRMATION :
				NotificationType.RESERVATION_CANCEL
		);
		notificationRepository.save(notification);

		log.info("예약 상태 변경 알림 저장 완료: reservationId={}, newStatus={}", reservation.getReservationId(), newStatus);
	}

	@Transactional(readOnly = true)
	public List<ReservationCreateResponse> getReservationsByUserAndStatus(Long userId, String status) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		List<Reservation> reservations;
		if (status != null) {
			status = status.toUpperCase();
			ReservationStatus reservationStatus;
			try {
				reservationStatus = ReservationStatus.valueOf(status.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
			}
			reservations = reservationRepository.findByUserAndStatus(user, reservationStatus);
		} else {
			reservations = reservationRepository.findByUser(user);
		}

		return reservations.stream()
			.map(reservation -> ReservationCreateResponse.builder()
				.reservationId(reservation.getReservationId())
				.restaurantName(reservation.getRestaurant().getRestaurantName())
				.username(user.getName())
				.reservationDate(reservation.getReservationDate())
				.reservationTime(reservation.getReservationSlot().getSlotTime())
				.partySize(reservation.getPartySize())
				.status(reservation.getStatus())
				.build())
			.collect(Collectors.toList());
	}
}
