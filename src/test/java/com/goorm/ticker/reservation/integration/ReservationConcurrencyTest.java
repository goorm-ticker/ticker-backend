package com.goorm.ticker.reservation.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.goorm.ticker.notification.repository.NotificationRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.fixture.ReservationSlotFixture;
import com.goorm.ticker.fixture.RestaurantFixture;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.dto.request.ReservationCreateRequest;
import com.goorm.ticker.reservation.dto.response.ReservationCreateResponse;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.ReservationSlotRepository;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@ActiveProfiles("test")
@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReservationConcurrencyTest {

	private static final int THREAD_COUNT = 100;

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private ReservationSlotRepository reservationSlotRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private NotificationRepository notificationRepository;

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
		List<User> users = new ArrayList<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			users.add(User.builder()
					.loginId("loginId" + (i))
					.name("name" + (i))
					.password("password" + (i))
					.build());
		}
		List<User> savedUsers = userRepository.saveAll(users);
		userRepository.flush();

		// 시작 ID 설정
		startUserId = savedUsers.get(0).getId();
		// 데이터 검증
		assertThat(restaurantRepository.count()).isGreaterThan(0);
		assertThat(reservationSlotRepository.count()).isGreaterThanOrEqualTo(4);
		assertThat(userRepository.count()).isEqualTo(THREAD_COUNT);
	}

	@AfterEach
	void afterEach() {
		notificationRepository.deleteAll();
		reservationRepository.deleteAll();
		reservationSlotRepository.deleteAll();
		restaurantRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("100명 유저 동시 예약 테스트 - 비관적 락 적용")
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
		for (long i = startUserId; i < startUserId + THREAD_COUNT; i++) {
			final long index = i;
			executorService.execute(() -> {
				try {
					ReservationCreateRequest request = ReservationCreateRequest.of(
							(index),
							restaurantId,
							reservationTime,
							reservationDate,
							partySize);
					reservationService.reserve(request, index);
					successCount.incrementAndGet();
				} catch (CustomException e) {
					failureCount.incrementAndGet();
					log.warn("[X] 실패 - 유저 ID: {} | 에러 코드: {} | {} ", (index), e.getErrorCode(),
							e.getErrorCode().getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();
		long endTime = System.currentTimeMillis();

		long totalReservations = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);

		logReservationResults(successCount, failureCount, totalReservations, "예약");
		log.info("실행 시간: {} ms", (endTime - startTime));

		assertThat(successCount.get()).isEqualTo(testSlots.get(0).getAvailablePartySize() / partySize);
		assertThat(totalReservations).isEqualTo(testSlots.get(0).getAvailablePartySize() / partySize);
	}

	@Test
	@DisplayName("예약과 취소가 동시에 실행되면서 정합성이 유지되는지 테스트")
	void testConcurrentReservationAndCancellation() throws InterruptedException, ExecutionException {
		int threadCount = 100; // 동시 실행할 요청 개수
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount); // 모든 작업이 끝날 때까지 대기

		Long restaurantId = restaurantInstant.getRestaurantId();
		LocalTime reservationTime = LocalTime.of(12, 0);
		LocalDate reservationDate = LocalDate.now();
		int partySize = 2;

		AtomicInteger successReservationCount = new AtomicInteger(0);
		AtomicInteger failureReservationCount = new AtomicInteger(0);
		AtomicInteger successCancelCount = new AtomicInteger(0);
		AtomicInteger failureCancelCount = new AtomicInteger(0);

		ConcurrentHashMap<Long, Long> successfulReservations = new ConcurrentHashMap<>();
		List<Long> reservationUserIds = new CopyOnWriteArrayList<>();
		List<Future<?>> reservationFutures = new ArrayList<>();

		// 미리 5명 예약 진행
		log.info("테스트 세팅 - 5명의 사용자가 미리 예약을 진행합니다.");
		for (long i = startUserId; i < startUserId + 5; i++) {
			final long userId = i;
			reservationFutures.add(executorService.submit(() -> {
				try {
					ReservationCreateRequest request = ReservationCreateRequest.of(
							userId, restaurantId, reservationTime, reservationDate, partySize);
					ReservationCreateResponse response = reservationService.reserve(request, userId);
					successfulReservations.put(userId, response.getReservationId());
					reservationUserIds.add(userId);
					successReservationCount.incrementAndGet();
				} catch (CustomException e) {
					failureReservationCount.incrementAndGet();
					log.warn("[X] 예약 실패 - 유저 ID: {} | {} {} ", userId, e.getErrorCode(),
							e.getErrorCode().getMessage());
				} finally {
					latch.countDown();
				}
			}));
		}

		// 모든 예약이 완료될 때까지 대기
		for (Future<?> future : reservationFutures) {
			future.get();
		}

		Awaitility.await()
				.atMost(5, TimeUnit.SECONDS)
				.pollInterval(500, TimeUnit.MILLISECONDS)
				.until(() -> reservationRepository.countByStatus(ReservationStatus.CONFIRMED) >= 5);

		log.info("테스트 시작 - 100명의 유저가 동시에 예약과 취소 요청을 보냅니다.");

		long startTime = System.currentTimeMillis();
		for (long i = startUserId; i < startUserId + threadCount; i++) {
			final long userId = i;
			executorService.execute(() -> {
				try {
					boolean isReservation = ThreadLocalRandom.current().nextBoolean(); // 랜덤하게 예약/취소 결정

					if (isReservation) {
						// 🟢 예약 요청
						ReservationCreateRequest request = ReservationCreateRequest.of(
								userId,
								restaurantId,
								reservationTime,
								reservationDate,
								partySize
						);
						ReservationCreateResponse response = reservationService.reserve(request, userId);
						successfulReservations.put(userId, response.getReservationId()); // 성공한 예약 저장
						successReservationCount.incrementAndGet();
						reservationUserIds.add(userId);
					} else {
						// 취소 요청 (랜덤한 유저 선택)
						if (successfulReservations.isEmpty() || reservationUserIds.isEmpty()) {
							log.warn("[X] 취소 실패 - 예약 없음");
							failureCancelCount.incrementAndGet();
							return;
						}
						Long randomUserId = reservationUserIds.get(
								ThreadLocalRandom.current().nextInt(reservationUserIds.size()));
						reservationUserIds.remove(randomUserId);
						Long reservationId = successfulReservations.remove(randomUserId);
						if (reservationId == null) {
							log.warn("[X] 취소 실패 - 예약 없음 (랜덤 유저 ID: {})", randomUserId);
							failureCancelCount.incrementAndGet();
							return;
						}
						reservationService.updateReservation(reservationId, "CANCELLED", randomUserId);
						successCancelCount.incrementAndGet();

					}
				} catch (CustomException e) {
					if (e.getErrorCode() == ErrorCode.PARTY_SIZE_EXCEEDED) {
						failureReservationCount.incrementAndGet();
					} else {
						failureCancelCount.incrementAndGet();
					}
					log.warn("[X] 예약 실패 - 유저 ID: {} | {} {}", userId,
							e.getErrorCode(), e.getErrorCode().getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();
		long endTime = System.currentTimeMillis();
		log.info("실행 시간: {} ms", (endTime - startTime));

		long totalReservations = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
		long totalCancelReservations = reservationRepository.countByStatus(ReservationStatus.CANCELLED);

		logReservationResults(successCancelCount, failureCancelCount, totalCancelReservations, "취소");
		logReservationResults(successReservationCount, failureReservationCount, totalReservations, "예약");

		// 검증
		assertThat(successReservationCount.get()).isGreaterThan(0);
		assertThat(successCancelCount.get()).isGreaterThan(0);
		assertThat(totalReservations).isLessThanOrEqualTo(testSlots.get(0).getAvailablePartySize() / partySize);
	}

	private void logReservationResults(
			AtomicInteger succssCount,
			AtomicInteger failureCount,
			long totalCount,
			String type
	) {
		log.info("---------------------------------------------");
		log.info("{} 성공: {}", type, succssCount.get());
		log.warn("{} 실패: {}", type, failureCount.get());

		log.info("최종 DB {} 건수: {}", type, totalCount);
	}
}
