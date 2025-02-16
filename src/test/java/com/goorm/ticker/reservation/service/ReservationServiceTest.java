package com.goorm.ticker.reservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	private ReservationStatusPublisher reservationStatusPublisher;

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
		user = UserFixture.USER_FIXTURE_1.createUserWithId(1L);
		reservationInstant = ReservationFixture.RESERVATION_FIXTURE_2.createReservation(restaurantInstant,
				reservationSlotInstant,
				user);
		reservationManual = ReservationFixture.RESERVATION_FIXTURE_1.createReservation(restaurantManual,
				reservationSlotManual,
				user);

		when(restaurantRepository.findById(anyLong()))
				.thenReturn(Optional.of(restaurantInstant));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(any(), anyLong()))
				.thenReturn(Optional.of(reservationSlotInstant));
		when(userRepository.findById(anyLong()))
				.thenReturn(Optional.of(user));
		when(reservationRepository.findById(anyLong()))
				.thenReturn(Optional.of(reservationInstant));
		when(reservationRepository.save(any(Reservation.class)))
				.thenAnswer(invocation -> {
					Reservation savedReservation = invocation.getArgument(0);
					ReflectionTestUtils.setField(savedReservation, "reservationId", 1L);
					return savedReservation;
				});
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
		when(reservationRepository.save(any(Reservation.class)))
				.thenAnswer(invocation -> {
					Reservation savedReservation = invocation.getArgument(0);
					ReflectionTestUtils.setField(savedReservation, "reservationId", 1L);
					return savedReservation;
				});

		// When
		ReservationCreateResponse response = reservationService.reserve(request, user.getId());

		// Then
		assertSoftly(softly -> {
			softly.assertThat(response).isNotNull();
			softly.assertThat(response.getRestaurantName()).isEqualTo(restaurantInstant.getRestaurantName());
			softly.assertThat(response.getUsername()).isEqualTo(user.getName());
			softly.assertThat(response.getPartySize()).isEqualTo(request.getPartySize());
			softly.assertThat(response.getReservationTime()).isEqualTo(reservationSlotInstant.getSlotTime());
			softly.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		});
		verify(reservationRepository, atMost(2)).save(any(Reservation.class));
		verify(reservationStatusPublisher, atLeastOnce())
				.publishReservationStatus(anyLong(), eq("CONFIRMED"));
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
		when(reservationRepository.save(any(Reservation.class)))
				.thenAnswer(invocation -> {
					Reservation savedReservation = invocation.getArgument(0);
					ReflectionTestUtils.setField(savedReservation, "reservationId", 1L);
					return savedReservation;
				});

		// When
		ReservationCreateResponse response = reservationService.reserve(request, user.getId());

		// Then
		assertSoftly(softly -> {
			softly.assertThat(response).isNotNull();
			softly.assertThat(response.getRestaurantName()).isEqualTo(restaurantManual.getRestaurantName());
			softly.assertThat(response.getUsername()).isEqualTo(user.getName());
			softly.assertThat(response.getPartySize()).isEqualTo(request.getPartySize());
			softly.assertThat(response.getReservationTime()).isEqualTo(reservationSlotManual.getSlotTime());
			softly.assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
		});
		verify(reservationRepository, atMost(2)).save(any(Reservation.class));
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
		Assertions.assertThatThrownBy(() -> reservationService.reserve(request, user.getId()))
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
		Assertions.assertThatThrownBy(() -> reservationService.reserve(request, user.getId()))
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
		Assertions.assertThatThrownBy(() -> reservationService.reserve(request, user.getId()))
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

		when(reservationRepository.findById(reservationManual.getReservationId()))
				.thenReturn(Optional.of(reservationManual));

		int initialAvailablePartySize = reservationSlotManual.getAvailablePartySize();
		when(userRepository.save(any())).thenReturn(user);
		// When
		ReservationCreateResponse response = reservationService.updateReservation(
				reservationManual.getReservationId(), "CONFIRMED", user.getId());

		// Then
		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		Assertions.assertThat(reservationManual.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		Assertions.assertThat(reservationSlotManual.getAvailablePartySize())
				.isEqualTo(initialAvailablePartySize - reservationManual.getPartySize());

		verify(reservationRepository, atMost(2)).findById(reservationInstant.getReservationId());
		verify(reservationRepository, atMost(2)).save(reservationInstant);
		verify(reservationStatusPublisher, atLeastOnce()).publishReservationStatus(
				reservationInstant.getReservationId(), "CONFIRMED"
		);
	}

	@DisplayName("예약 대기 상태에서 취소 상태로 변경합니다.")
	@Test
	void testReserveAndThenCancel() {
		// Given - 예약 생성
		ReservationCreateRequest request = ReservationCreateRequest.of(
				1L, restaurantManual.getRestaurantId(), reservationSlotManual.getSlotTime(),
				reservationManual.getReservationDate(),
				reservationManual.getPartySize());

		when(restaurantRepository.findById(request.getRestaurantId())).thenReturn(Optional.of(restaurantManual));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
				request.getReservationTime(), request.getRestaurantId())).thenReturn(Optional.of(reservationSlotManual));
		when(userRepository.findById(request.getUserId())).thenReturn(Optional.of(user));

		ReservationCreateResponse reservationResponse = reservationService.reserve(request, user.getId());

		// Then - 예약이 정상적으로 생성되었는지 확인
		assertThat(reservationResponse).isNotNull();
		assertThat(reservationResponse.getStatus()).isEqualTo(ReservationStatus.PENDING);

		// Given - 예약이 생성된 상태에서 취소 진행
		Long reservationId = reservationResponse.getReservationId();
		Long userId = user.getId();

		when(reservationRepository.findById(reservationId)).thenReturn(
				Optional.of(Reservation.of(restaurantManual, reservationSlotManual, request.getReservationDate(),
						user, request.getPartySize(), ReservationStatus.PENDING)));

		int initialAvailablePartySize = reservationSlotManual.getAvailablePartySize();

		// When - 예약 취소
		ReservationCreateResponse cancelResponse = reservationService.updateReservation(reservationId, "CANCELLED", userId);

		// Then - 상태가 CANCELLED로 변경되었는지 확인
		assertThat(cancelResponse).isNotNull();
		assertThat(cancelResponse.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
		Assertions.assertThat(reservationSlotManual.getAvailablePartySize()).isEqualTo(
				initialAvailablePartySize
		);

		verify(reservationRepository, times(1)).saveAndFlush(any(Reservation.class));

		verify(reservationRepository, atMost(2)).findById(reservationId);
		verify(reservationRepository, atMost(2)).save(any(Reservation.class));
		verify(reservationStatusPublisher, atLeastOnce()).publishReservationStatus(anyLong(), eq("CANCELLED"));

		// Then - 예약 취소 검증
		assertSoftly(softly -> {
			softly.assertThat(cancelResponse).isNotNull();
			softly.assertThat(cancelResponse.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
			softly.assertThat(reservationSlotManual.getAvailablePartySize()).isEqualTo(initialAvailablePartySize);
		});
	}


	@DisplayName("예약 확정에서 취소로 변경하여 예약 가능 인원이 증가합니다.")
	@Test
	void testUpdateReservationStatusFromConfirmedToCancelled() {
		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
				1L, restaurantInstant.getRestaurantId(), reservationSlotInstant.getSlotTime(),
				reservationInstant.getReservationDate(),
				reservationInstant.getPartySize());
		log.info("reservationInstant.getPartySize(): {}", reservationInstant.getPartySize());


		doNothing().when(reservationStatusPublisher).publishReservationStatus(anyLong(), anyString());

		// When
		when(restaurantRepository.findById(request.getRestaurantId())).thenReturn(Optional.of(restaurantInstant));
		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
				request.getReservationTime(), request.getRestaurantId())).thenReturn(Optional.of(reservationSlotInstant));
		when(userRepository.findById(request.getUserId())).thenReturn(Optional.of(user));

		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
			Reservation reservation = invocation.getArgument(0);
			ReflectionTestUtils.setField(reservation, "reservationId", 1L);

			if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
				reservationSlotInstant.updateAvailablePartySize(
						reservationSlotInstant.getAvailablePartySize() - reservation.getPartySize());
			}
			return reservation;
		});

		int initialAvailablePartySize = reservationSlotInstant.getAvailablePartySize();
		log.info("initialAvailablePartySize : {}", initialAvailablePartySize);
		ReservationCreateResponse reservationResponse = reservationService.reserve(request, user.getId());

		// Then - 예약이 정상적으로 생성되었는지 확인
		assertThat(reservationResponse).isNotNull();
		assertThat(reservationResponse.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

		// Given - 예약이 생성된 상태에서 취소 진행
		Long reservationId = reservationResponse.getReservationId();
		Long userId = user.getId();
		when(reservationRepository.findById(reservationId)).thenAnswer(invocation -> {
			Reservation updatedReservation = Reservation.of(
					restaurantInstant,
					reservationSlotInstant,
					request.getReservationDate(),
					user,
					request.getPartySize(),
					ReservationStatus.CONFIRMED);
			if (updatedReservation.getStatus() == ReservationStatus.CONFIRMED) {
				reservationSlotInstant.updateAvailablePartySize(
						reservationSlotInstant.getAvailablePartySize() + updatedReservation.getPartySize());
			}
			ReflectionTestUtils.setField(updatedReservation, "reservationId", reservationId);
			return Optional.of(updatedReservation);
		});

		when(reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
				request.getReservationTime(), request.getRestaurantId())).thenReturn(Optional.of(reservationSlotInstant));

		ReservationCreateResponse response = reservationService.updateReservation(reservationId,
				"CANCELLED", userId);

		ReservationSlot updatedSlot = reservationSlotRepository.findBySlotTimeAndRestaurantIdWithLock(
				request.getReservationTime(), request.getRestaurantId()).orElseThrow();


		// Then
		Assertions.assertThat(response).isNotNull();
		Assertions.assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);


		verify(reservationRepository, atMost(2)).findById(reservationInstant.getReservationId());
		verify(reservationStatusPublisher, atMost(1)).publishReservationStatus(anyLong(), anyString());
		// 예약 취소 후 가용 인원 증가 확인
		assertThat(updatedSlot.getAvailablePartySize())
				.isEqualTo(initialAvailablePartySize); // 원래 상태로 복구됨
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
						() -> reservationService.updateReservation(reservation3.getReservationId(), "CANCELLED",
								user.getId()))
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
						() -> reservationService.updateReservation(reservation4.getReservationId(), "CANCELLED",
								user.getId()))
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
		Assertions.assertThatThrownBy(() -> reservationService.updateReservation(reservationId, invalidStatus,
						user.getId()))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException customException = (CustomException)ex;
					assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
					assertThat(customException.getErrorCode().getMessage()).isEqualTo("존재하지 않는 예약 상태입니다.");
					assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				});

		// Verify
		verify(reservationRepository, times(1)).findById(reservationId);
		verifyNoMoreInteractions(reservationRepository);
	}

	@Test
	@DisplayName("다른 사용자의 예약 상태 변경 시 예외 발생")
	void testUpdateReservation_UnauthorizedUser() {
		Long reservationId = reservationInstant.getReservationId();
		Long unauthorizedUserId = 999L; // 예약한 사용자가 아닌 다른 ID

		// When & Then
		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservationInstant));
		Assertions.assertThatThrownBy(() -> reservationService.updateReservation(reservationId, "CONFIRMED",
						unauthorizedUserId))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException customException = (CustomException)ex;
					assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN_RESERVATION_ACCESS);
					assertThat(customException.getErrorCode().getMessage()).isEqualTo("해당 예약에 접근할 수 없습니다.");
					assertThat(customException.getErrorCode().getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
				});
	}

	@DisplayName("유효한 사용자 ID와 특정 상태값을 입력하면 해당 상태의 예약만 조회된다.")
	@Test
	void testGetReservationsByUserAndStatus_SuccessWithStatus() {
		// Given
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(reservationRepository.findByUserAndStatus(user, ReservationStatus.CONFIRMED))
				.thenReturn(List.of(reservationInstant));
		log.info("예약 상태: {}", reservationInstant.getStatus());

		// When
		List<ReservationCreateResponse> responses = reservationService.getReservationsByUserAndStatus(
				user.getId(), "CONFIRMED");

		// Then
		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
	}

	@DisplayName("유효한 사용자 ID를 입력하지만 상태값을 입력하지 않으면 모든 예약이 조회된다.")
	@Test
	void testGetReservationsByUserAndStatus_SuccessWithoutStatus() {
		// Given
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(reservationRepository.findByUser(user))
				.thenReturn(List.of(reservationInstant, reservationManual));

		// When
		List<ReservationCreateResponse> responses = reservationService.getReservationsByUserAndStatus(
				user.getId(), null);

		// Then
		assertThat(responses).hasSize(2);
	}

	@DisplayName("유효하지 않은 사용자 ID를 입력하면 예외가 발생한다.")
	@Test
	void testGetReservationsByUserAndStatus_FailsWithInvalidUser() {
		// Given
		Long invalidUserId = 999L;
		when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(
				() -> reservationService.getReservationsByUserAndStatus(invalidUserId, "CONFIRMED"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException customException = (CustomException)ex;
					Assertions.assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_USER);
					Assertions.assertThat(customException.getErrorCode().getMessage())
							.isEqualTo(ErrorCode.NOT_FOUND_USER.getMessage());
				});
	}

	@DisplayName("유효하지 않은 상태값을 입력하면 예외가 발생한다.")
	@Test
	void testGetReservationsByUserAndStatus_FailsWithInvalidStatus() {
		// Given
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		// When & Then
		assertThatThrownBy(
				() -> reservationService.getReservationsByUserAndStatus(user.getId(), "INVALID_STATUS"))
				.isInstanceOf(CustomException.class)
				.satisfies(ex -> {
					CustomException customException = (CustomException)ex;
					Assertions.assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS);
					Assertions.assertThat(customException.getErrorCode().getMessage())
							.isEqualTo(ErrorCode.INVALID_RESERVATION_STATUS.getMessage());
				});
	}

	@DisplayName("사용자가 예약을 하나도 하지 않은 경우, 빈 리스트가 반환된다.")
	@Test
	void testGetReservationsByUserAndStatus_ReturnsEmptyList() {
		// Given
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(reservationRepository.findByUser(user)).thenReturn(List.of());

		// When
		List<ReservationCreateResponse> responses = reservationService.getReservationsByUserAndStatus(
				user.getId(), null);

		// Then
		assertThat(responses).isEmpty();
	}
}
