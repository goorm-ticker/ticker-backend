package com.goorm.ticker.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.fixture.ReservationFixture;
import com.goorm.ticker.fixture.ReservationSlotFixture;
import com.goorm.ticker.fixture.RestaurantFixture;
import com.goorm.ticker.fixture.UserFixture;
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

class ReservationServiceTest {

	@InjectMocks
	private ReservationService reservationService;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private RestaurantRepository restaurantRepository;

	@Mock
	private ReservationSlotRepository reservationSlotRepository;

	@Mock
	private UserRepository userRepository;

	private Restaurant restaurantInstant;
	private Restaurant restaurantManual;
	private Reservation reservationInstant;
	private Reservation reservationManual;
	private ReservationSlot reservationSlotInstant;
	private ReservationSlot reservationSlotManual;
	private User user;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		restaurantInstant = RestaurantFixture.RESTAURANT_FIXTURE_1.createRestaurant();
		restaurantManual = RestaurantFixture.RESTAURANT_FIXTURE_2.createRestaurant();
		reservationSlotInstant = ReservationSlotFixture.SLOT_FIXTURE_1.createSlot(restaurantInstant);
		reservationSlotManual = ReservationSlotFixture.SLOT_FIXTURE_1.createSlot(restaurantManual);
		user = UserFixture.USER_FIXTURE_1.createUser();
		reservationInstant = ReservationFixture.RESERVATION_FIXTURE_1.createReservation(restaurantInstant,
			reservationSlotInstant,
			user);
		reservationManual = ReservationFixture.RESERVATION_FIXTURE_1.createReservation(restaurantManual,
			reservationSlotManual,
			user);
	}

	@DisplayName("단일 예약을 성공합니다. -> 즉시 예약 확정 정책")
	@Test
	void testReserveSuccessInstant() {
		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(), restaurantInstant.getRestaurantId(), reservationSlotInstant.getSlotTime(),
			reservationInstant.getReservationDate(),
			reservationInstant.getPartySize());

		when(restaurantRepository.findById(request.getRestaurantId())).thenReturn(Optional.of(restaurantInstant));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
			request.getReservationTime(), request.getRestaurantId())).thenReturn(Optional.of(reservationSlotInstant));
		when(userRepository.findById(request.getUserId())).thenReturn(Optional.of(user));
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ReservationCreateResponse response = reservationService.reserve(request);

		// Then
		assertSoftly(softly -> {
			softly.assertThat(response).isNotNull();
			softly.assertThat(response.getRestaurantName()).isEqualTo(restaurantInstant.getRestaurantName());
			softly.assertThat(response.getUsername()).isEqualTo(user.getName());
			softly.assertThat(response.getPartySize()).isEqualTo(request.getPartySize());
			softly.assertThat(response.getReservationTime()).isEqualTo(reservationSlotInstant.getSlotTime());
			softly.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		});
		verify(reservationRepository, times(1)).save(any(Reservation.class));
	}

	@DisplayName("단일 예약을 성공합니다. -> 수동 예약 확정 정책")
	@Test
	void testReserveSuccessManual() {
		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(), restaurantManual.getRestaurantId(), reservationSlotManual.getSlotTime(),
			reservationManual.getReservationDate(),
			reservationManual.getPartySize());

		when(restaurantRepository.findById(request.getRestaurantId())).thenReturn(Optional.of(restaurantManual));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
			request.getReservationTime(), request.getRestaurantId())).thenReturn(Optional.of(reservationSlotManual));
		when(userRepository.findById(request.getUserId())).thenReturn(Optional.of(user));
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		ReservationCreateResponse response = reservationService.reserve(request);

		// Then
		assertSoftly(softly -> {
			softly.assertThat(response).isNotNull();
			softly.assertThat(response.getRestaurantName()).isEqualTo(restaurantManual.getRestaurantName());
			softly.assertThat(response.getUsername()).isEqualTo(user.getName());
			softly.assertThat(response.getPartySize()).isEqualTo(request.getPartySize());
			softly.assertThat(response.getReservationTime()).isEqualTo(reservationSlotManual.getSlotTime());
			softly.assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
		});
		verify(reservationRepository, times(1)).save(any(Reservation.class));
	}

	@DisplayName("음식점 정보가 없는 경우 예외를 발생시킵니다.")
	@Test
	void testReserveFailsWhenRestaurantNotFound() {
		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(),
			999L,
			reservationSlotInstant.getSlotTime(),
			reservationInstant.getReservationDate(),
			reservationInstant.getPartySize()
		);

		when(restaurantRepository.findById(request.getRestaurantId()))
			.thenReturn(Optional.empty());

		// When & Then
		Assertions.assertThatThrownBy(() -> reservationService.reserve(request))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException)ex;
				assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.RESTAURANT_NOT_FOUND);
				assertThat(customException.getErrorCode().getMessage()).isEqualTo("존재하지 않는 음식점입니다.");
				assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
			});

		verify(restaurantRepository, times(1)).findById(request.getRestaurantId());
		verifyNoInteractions(reservationSlotRepository, reservationRepository);

	}

	@DisplayName("음식점 예약 시간대 정보가 없는 경우 예외를 발생시킵니다.")
	@Test
	void testReserveFailsWhenSlotNotFound() {
		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(),
			restaurantInstant.getRestaurantId(),
			reservationSlotInstant.getSlotTime(),
			reservationInstant.getReservationDate(),
			reservationInstant.getPartySize()
		);

		when(restaurantRepository.findById(request.getRestaurantId()))
			.thenReturn(Optional.of(restaurantInstant));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
			request.getReservationTime(), request.getRestaurantId()))
			.thenReturn(Optional.empty());

		// When & Then
		Assertions.assertThatThrownBy(() -> reservationService.reserve(request))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException)ex;
				assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_SLOT_NOT_FOUND);
				assertThat(customException.getErrorCode().getMessage()).isEqualTo("존재하지 않는 예약 시간대입니다.");
				assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
			});

		verify(restaurantRepository, times(1)).findById(request.getRestaurantId());
		verify(reservationSlotRepository, times(1))
			.findBySlotTimeAndRestaurantIdWithLock(request.getReservationTime(), request.getRestaurantId());
		verifyNoInteractions(userRepository, reservationRepository);
	}

	@Transactional
	@DisplayName("예약 가능 인원보다 예약 인원이 많은 경우 예외를 발생시킵니다.")
	@Test
	void testReserveFailsWhenPartySizeExceeded() {
		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(),
			restaurantInstant.getRestaurantId(),
			reservationSlotInstant.getSlotTime(),
			reservationInstant.getReservationDate(),
			reservationSlotInstant.getAvailablePartySize() + 1
		);

		when(restaurantRepository.findById(request.getRestaurantId()))
			.thenReturn(Optional.of(restaurantInstant));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
			request.getReservationTime(), request.getRestaurantId()))
			.thenReturn(Optional.of(reservationSlotInstant));
		when(userRepository.findById(request.getUserId()))
			.thenReturn(Optional.of(user));

		// When & Then
		Assertions.assertThatThrownBy(() -> reservationService.reserve(request))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException)ex;
				assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.PARTY_SIZE_EXCEEDED);
				assertThat(customException.getErrorCode().getMessage()).isEqualTo("해당 시간대에 예약 가능한 인원이 초과되었습니다.");
				assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
			});

		// Verify
		verify(restaurantRepository, times(1)).findById(request.getRestaurantId());
		verify(reservationSlotRepository, times(1))
			.findBySlotTimeAndRestaurantIdWithLock(request.getReservationTime(), request.getRestaurantId());
		verifyNoInteractions(reservationRepository);
	}

	@DisplayName("예약 대기에서 확정으로 상태를 변경합니다.")
	@Test
	void testUpdateReservationStatusConfirmed() {
		// Given

		when(reservationRepository.findById(reservationInstant.getReservationId()))
			.thenReturn(Optional.of(reservationInstant));

		int initialAvailablePartySize = reservationSlotInstant.getAvailablePartySize();

		// When
		ReservationCreateResponse response = reservationService.updateReservation(
			reservationInstant.getReservationId(), "CONFIRMED");

		// Then
		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		Assertions.assertThat(reservationInstant.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		Assertions.assertThat(reservationSlotInstant.getAvailablePartySize())
			.isEqualTo(initialAvailablePartySize - reservationInstant.getPartySize());

		verify(reservationRepository, times(1)).findById(reservationInstant.getReservationId());
	}

	@DisplayName("예약 대기 상태에서 취소 상태로 변경합니다.")
	@Test
	void testUpdateReservationStatusToCancelled() {
		// Given
		Long reservationId = reservationManual.getReservationId();

		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservationManual));

		int initialAvailablePartySize = reservationSlotManual.getAvailablePartySize();
		// When
		ReservationCreateResponse response = reservationService.updateReservation(reservationId, "CANCELLED");

		// Then
		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

		Assertions.assertThat(reservationManual.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		Assertions.assertThat(reservationSlotManual.getAvailablePartySize()).isEqualTo(
			initialAvailablePartySize
		);

		verify(reservationRepository, times(1)).findById(reservationId);
	}

	@DisplayName("예약 확정에서 취소로 변경하여 예약 가능 인원이 증가합니다.")
	@Test
	void testUpdateReservationStatusFromConfirmedToCancelled() {
		// Given
		ReservationSlot reservationSlot2 = ReservationSlotFixture.SLOT_FIXTURE_2.createSlot(restaurantInstant);
		Reservation reservation2 = ReservationFixture.RESERVATION_FIXTURE_2.createReservation(restaurantInstant,
			reservationSlot2, user);
		when(reservationRepository.findById(reservation2.getReservationId())).thenReturn(Optional.of(reservation2));
		int initialAvailablePartySize = reservationSlot2.getAvailablePartySize();

		// When
		ReservationCreateResponse response = reservationService.updateReservation(reservation2.getReservationId(),
			"CANCELLED");

		// Then
		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		Assertions.assertThat(reservationSlot2.getAvailablePartySize())
			.isEqualTo(initialAvailablePartySize + reservation2.getPartySize());

		verify(reservationRepository, times(1)).findById(reservation2.getReservationId());
	}

	@DisplayName("예약 취소에서 취소로 변경 시 예외를 발생시킵니다.")
	@Test
	void testUpdateReservationStatusFailsWhenAlreadyCancelled() {
		// Given
		ReservationSlot reservationSlot3 = ReservationSlotFixture.SLOT_FIXTURE_3.createSlot(restaurantInstant);
		Reservation reservation3 = ReservationFixture.RESERVATION_FIXTURE_3.createReservation(restaurantInstant,
			reservationSlot3, user);
		when(reservationRepository.findById(reservation3.getReservationId())).thenReturn(Optional.of(reservation3));

		// When
		Assertions.assertThatThrownBy(
				() -> reservationService.updateReservation(reservation3.getReservationId(), "CANCELLED"))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException)ex;
				Assertions.assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_UPDATED);
				Assertions.assertThat(customException.getErrorCode().getMessage())
					.isEqualTo(ErrorCode.RESERVATION_ALREADY_UPDATED.getMessage());
				Assertions.assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
			});

		// Then
		verify(reservationRepository, times(1)).findById(reservation3.getReservationId());
		verifyNoMoreInteractions(reservationRepository);
	}

	@DisplayName("예약 입장 상태에서 취소로 변경 시 예외를 발생시킵니다.")
	@Test
	void testUpdateReservationStatusFailsWhenEnteredToCancelled() {
		// Given
		ReservationSlot reservationSlot4 = ReservationSlotFixture.SLOT_FIXTURE_4.createSlot(restaurantInstant);
		Reservation reservation4 = ReservationFixture.RESERVATION_FIXTURE_4.createReservation(restaurantInstant,
			reservationSlot4, user);
		when(reservationRepository.findById(reservation4.getReservationId())).thenReturn(Optional.of(reservation4));

		// When
		Assertions.assertThatThrownBy(
				() -> reservationService.updateReservation(reservation4.getReservationId(), "CANCELLED"))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException)ex;
				Assertions.assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_UPDATED);
				Assertions.assertThat(customException.getErrorCode().getMessage())
					.isEqualTo(ErrorCode.RESERVATION_ALREADY_UPDATED.getMessage());
				Assertions.assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
			});

		// Then
		verify(reservationRepository, times(1)).findById(reservation4.getReservationId());
		verifyNoMoreInteractions(reservationRepository);
	}

	@DisplayName("유효하지 않은 예약 상태를 전달하면 예외를 발생시킵니다.")
	@Test
	void testUpdateReservationInvalidStatus() {
		// Given
		Long reservationId = reservationInstant.getReservationId();
		String invalidStatus = "INVALID_STATUS";
		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservationInstant));

		// When & Then
		Assertions.assertThatThrownBy(() -> reservationService.updateReservation(reservationId, invalidStatus))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException)ex;
				assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
				assertThat(customException.getErrorCode().getMessage()).isEqualTo("존재하지 않는 예약 상태입니다.");
				assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
			});

		// Verify
		verify(reservationRepository, times(1)).findById(reservationId);
		verifyNoMoreInteractions(reservationRepository);
	}
}
