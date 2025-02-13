package com.goorm.ticker.reservation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.fixture.ReservationSlotFixture;
import com.goorm.ticker.fixture.RestaurantFixture;
import com.goorm.ticker.fixture.UserFixture;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(ReservationController.class)
@ExtendWith(MockitoExtension.class)
public class ReservationControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReservationService reservationService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private ReservationRepository reservationRepository;

	private MockHttpSession session;
	private Restaurant restaurant;
	private ReservationSlot reservationSlot;
	private User user;
	private Reservation reservation;

	@BeforeEach
	void setUp() {
		// 세션 초기화
		session = new MockHttpSession();

		// 사용자 데이터 설정 및 저장
		user = UserFixture.USER_FIXTURE_1.createUserWithId(1L);
		when(userRepository.findById(user.getId())).thenReturn(java.util.Optional.of(user));
		session.setAttribute("userId", user.getId());

		// 식당 및 예약 슬롯 데이터 설정
		restaurant = RestaurantFixture.RESTAURANT_FIXTURE_1.createRestaurant();
		reservationSlot = ReservationSlotFixture.SLOT_FIXTURE_1.createSlot(restaurant);
	}

	@DisplayName("POST /reservations - 예약 생성 성공")
	@Test
	void testCreateReservation() throws Exception {

		// Given
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(),
			restaurant.getRestaurantId(),
			reservationSlot.getSlotTime(),
			LocalDate.parse("2025-02-15"),
			2
		);

		ReservationCreateResponse response = ReservationCreateResponse.builder()
			.reservationId(1L)
			.restaurantName(restaurant.getRestaurantName())
			.username(user.getName())
			.partySize(request.getPartySize())
			.reservationDate(request.getReservationDate())
			.reservationTime(reservationSlot.getSlotTime())
			.status(ReservationStatus.CONFIRMED)
			.build();

		when(reservationService.reserve(any(ReservationCreateRequest.class), anyLong()))
			.thenReturn(response);

		// When & Then
		MockHttpServletResponse responseEntity = mockMvc.perform(post("/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.session(session))
			.andReturn().getResponse();

		// Then
		assertThat(responseEntity.getStatus()).isEqualTo(HttpStatus.OK.value());
		verify(reservationService, times(1)).reserve(any(ReservationCreateRequest.class), anyLong());

		assertThat(responseEntity.getContentAsString()).isNotEmpty();

		String responseBody = responseEntity.getContentAsString();
		ReservationCreateResponse reservationResponse = objectMapper.readValue(responseBody,
			ReservationCreateResponse.class);
		assertThat(reservationResponse.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
	}

	@DisplayName("PATCH /reservations/{reservationId} - 예약 상태 업데이트 성공")
	@Test
	void testUpdateReservationStatus() throws Exception {
		// Given
		Long reservationId = 1L;
		String status = "CONFIRMED";
		Map<String, String> updateRequest = Map.of("status", status);

		ReservationCreateResponse response = ReservationCreateResponse.builder()
			.reservationId(reservationId)
			.restaurantName(restaurant.getRestaurantName())
			.username(user.getName())
			.reservationTime(reservationSlot.getSlotTime())
			.reservationDate(LocalDate.parse("2025-02-15"))
			.partySize(2)
			.status(ReservationStatus.CONFIRMED)
			.build();

		when(reservationService.updateReservation(eq(reservationId), eq(status), eq(user.getId())))
			.thenReturn(response);

		// When & Then
		mockMvc.perform(patch("/reservations/{reservationId}", reservationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest))
				.session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationId").value(response.getReservationId()))
			.andExpect(jsonPath("$.restaurantName").value(response.getRestaurantName()))
			.andExpect(jsonPath("$.username").value(response.getUsername()))
			.andExpect(jsonPath("$.partySize").value(response.getPartySize()))
			.andExpect(jsonPath("$.status").value(response.getStatus().name()));

		verify(reservationService, times(1))
			.updateReservation(eq(reservationId), eq(status), eq(user.getId()));
	}

	@DisplayName("PATCH /reservations/{reservationId} - 예약 상태 업데이트 실패 (권한 없음 403)")
	@Test
	void testUpdateReservationStatus_Fail_Unauthorized() throws Exception {
		// Given
		Long reservationId = 1L;
		String status = "CANCELLED";
		Map<String, String> updateRequest = Map.of("status", status);

		// 세션 내 유저 ID와 예약의 유저 ID가 다름
		Long otherUserId = 999L;
		session.setAttribute("userId", otherUserId);

		when(reservationService.updateReservation(eq(reservationId), eq(status), eq(otherUserId)))
			.thenThrow(new CustomException(ErrorCode.FORBIDDEN_RESERVATION_ACCESS));

		// When & Then
		mockMvc.perform(patch("/reservations/{reservationId}", reservationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest))
				.session(session))
			.andExpect(status().isForbidden()); // 권한 없음

		verify(reservationService, times(1))
			.updateReservation(eq(reservationId), eq(status), eq(otherUserId));
	}

	@DisplayName("POST /reservations - 세션 없음 (401 Unauthorized)")
	@Test
	void testCreateReservation_Fail_NoSession() throws Exception {
		// Given
		session.setAttribute("userId", null);
		ReservationCreateRequest request = ReservationCreateRequest.of(
			user.getId(),
			restaurant.getRestaurantId(),
			reservationSlot.getSlotTime(),
			LocalDate.parse("2025-02-15"),
			2
		);

		// When & Then
		mockMvc.perform(post("/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}
}
