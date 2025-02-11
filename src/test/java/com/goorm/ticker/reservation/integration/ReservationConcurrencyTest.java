package com.goorm.ticker.reservation.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.fixture.ReservationSlotFixture;
import com.goorm.ticker.fixture.RestaurantFixture;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.ReservationSlotRepository;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class ReservationConcurrencyTest {

	private static final int THREAD_COUNT = 1000;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private ReservationSlotRepository reservationSlotRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private UserRepository userRepository;

	private Restaurant restaurantInstant;
	private Long startUserId;
	private List<ReservationSlot> testSlots = new ArrayList<>();

	@BeforeEach
	void setUp() {
		// 테스트용 음식점 저장
		restaurantInstant = restaurantRepository.save(RestaurantFixture.RESTAURANT_FIXTURE_1.createRestaurant());

		// 테스트용 예약 슬롯 저장
		for (ReservationSlotFixture slotFixture : ReservationSlotFixture.values()) {
			ReservationSlot slot = slotFixture.createSlot(restaurantInstant);
			reservationSlotRepository.save(slot);
			testSlots.add(slot);
		}
		for (int i = 0; i < THREAD_COUNT; i++) {
			User user = User.builder()
				.loginId("loginId" + (i))
				.name("name" + (i))
				.password("password" + (i))
				.build();

			User savedUser = userRepository.save(user);
			if (i == 0) {
				startUserId = savedUser.getId();
			}
		}
		userRepository.flush();

		// 데이터 검증
		assertThat(restaurantRepository.count()).isGreaterThan(0);
		assertThat(reservationSlotRepository.count()).isGreaterThanOrEqualTo(4);
		assertThat(userRepository.count()).isEqualTo(THREAD_COUNT);
	}

	@AfterEach
	void afterEach() {
		reservationRepository.deleteAll();
		reservationSlotRepository.deleteAll();
		restaurantRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("1000명 유저 동시 예약 테스트")
	void testConcurrentReservationsWithoutLock() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
		long startTime = System.currentTimeMillis();
		Long restaurantId = restaurantInstant.getRestaurantId();
		LocalTime reservationTime = LocalTime.of(12, 0);
		LocalDate reservationDate = LocalDate.now();

		AtomicInteger successCount = new AtomicInteger(0); // 성공한 예약 수
		AtomicInteger failureCount = new AtomicInteger(0); // 실패한 예약 수
		log.info("restaurantId : {}", restaurantId);
		log.info("reservationTime : {}", reservationTime);
		log.info("reservationDate : {}", reservationDate);
		log.info("테스트 시작 - 100명의 유저가 동시에 예약 요청을 보냅니다.");
		log.info("-----------------------------------------------------");
		int partySize = 2;
		for (long i = startUserId; i <= startUserId + THREAD_COUNT; i++) {
			final long index = i;
			executorService.execute(() -> {
				try {
					ReservationCreateRequest request = ReservationCreateRequest.of(
						(index),
						restaurantId,
						reservationTime,
						reservationDate,
						partySize);
					reservationService.reserve(request);
					successCount.incrementAndGet();
					log.info("[O] 예약 성공 - 유저 ID: {} | 예약 일시 : {} {}", (index), reservationDate,
						reservationTime);
				} catch (CustomException e) {
					failureCount.incrementAndGet();
					log.warn("[X] 예약 실패 - 유저 ID: {} | 에러 코드: {}", (index), e.getErrorCode());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();
		log.info("--------------------------------------------");
		long endTime = System.currentTimeMillis();

		log.info("비관적 락 실행 시간: {} ms", (endTime - startTime));
		log.info("예약 성공: {}", successCount.get());
		log.warn("예약 실패: {}", failureCount.get());

		// DB에서 실제 예약된 건수 확인
		long totalReservations = reservationRepository.count();
		log.info("실제 DB 예약 건수: {}", totalReservations);

		assertThat(successCount.get()).isEqualTo(testSlots.get(0).getAvailablePartySize() / partySize);
		assertThat(totalReservations).isEqualTo(testSlots.get(0).getAvailablePartySize() / partySize);
	}
}
