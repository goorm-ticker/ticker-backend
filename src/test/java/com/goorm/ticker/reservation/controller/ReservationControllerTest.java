package com.goorm.ticker.reservation.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.fixture.ReservationFixture;
import com.goorm.ticker.fixture.ReservationSlotFixture;
import com.goorm.ticker.fixture.RestaurantFixture;
import com.goorm.ticker.fixture.UserFixture;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.service.ReservationService;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.user.entity.User;

@WebMvcTest(ReservationController.class)
@ExtendWith(MockitoExtension.class)
public class ReservationControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ReservationService reservationService;

	private final RestaurantFixture restaurantFixture = RestaurantFixture.RESTAURANT_FIXTURE_1;
	private final ReservationSlotFixture slotFixture = ReservationSlotFixture.SLOT_FIXTURE_1;
	private final UserFixture userFixture = UserFixture.USER_FIXTURE_1;
	private final ReservationFixture reservationFixture = ReservationFixture.RESERVATION_FIXTURE_1;

	@DisplayName("POST /api/reservations - 예약 생성 성공")
	@Test
	void testCreateReservation() throws Exception {
		// Given
		Restaurant restaurant = restaurantFixture.createRestaurant();
		ReservationSlot reservationSlot = slotFixture.createSlot(restaurant);
		User user = userFixture.createUser();
		Reservation reservation = reservationFixture.createReservation(restaurant, reservationSlot, user);

		ReservationCreateRequest request = ReservationCreateRequest.of(
			1L,
			1L,
			reservationSlot.getSlotTime(),
			reservation.getReservationDate(),
			reservation.getPartySize()
		);

		ReservationCreateResponse response = ReservationCreateResponse.builder()
			.reservationId(1L)
			.restaurantName(restaurant.getRestaurantName())
			.username(user.getName())
			.partySize(reservation.getPartySize())
			.reservationDate(reservation.getReservationDate())
			.reservationTime(reservationSlot.getSlotTime())
			.status(reservation.getStatus())
			.build();

		when(reservationService.reserve(any())).thenReturn(response);

		// When & Then
		mockMvc.perform(post("/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationId").value(response.getReservationId()))
			.andExpect(jsonPath("$.restaurantName").value(response.getRestaurantName()))
			.andExpect(jsonPath("$.username").value(response.getUsername()))
			.andExpect(jsonPath("$.partySize").value(response.getPartySize()))
			.andExpect(jsonPath("$.status").value(response.getStatus().name()));

		verify(reservationService, times(1)).reserve(any());
	}

	@DisplayName("PATCH /reservations/{reservationId} - 예약 상태 업데이트 성공")
	@Test
	void testUpdateReservationStatus() throws Exception {
		// Given
		Restaurant restaurant = restaurantFixture.createRestaurant();
		ReservationSlot reservationSlot = slotFixture.createSlot(restaurant);
		User user = userFixture.createUser();
		Reservation reservation = reservationFixture.createReservation(restaurant, reservationSlot, user);

		Long reservationId = 1L;
		String status = "CONFIRMED";
		Map<String, String> updateRequest = Map.of("status", status);

		ReservationCreateResponse response = ReservationCreateResponse.builder()
			.reservationId(reservationId)
			.restaurantName(restaurant.getRestaurantName())
			.username(user.getName())
			.reservationTime(reservationSlot.getSlotTime())
			.reservationDate(reservation.getReservationDate())
			.partySize(reservation.getPartySize())
			.status(ReservationStatus.CONFIRMED)
			.build();

		when(reservationService.updateReservation(eq(reservationId), eq(status))).thenReturn(response);

		// When & Then
		mockMvc.perform(patch("/reservations/{reservationId}", reservationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reservationId").value(response.getReservationId()))
			.andExpect(jsonPath("$.restaurantName").value(response.getRestaurantName()))
			.andExpect(jsonPath("$.username").value(response.getUsername()))
			.andExpect(jsonPath("$.partySize").value(response.getPartySize()))
			.andExpect(jsonPath("$.status").value(response.getStatus().name()));

		verify(reservationService, times(1)).updateReservation(eq(reservationId), eq(status));
	}
}
