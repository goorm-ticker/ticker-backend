package com.goorm.ticker.reservation.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@ActiveProfiles("test")
@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserDummyDataTest {

	private static final int THREAD_COUNT = 100;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@AfterEach
	void cleanUp() {
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("user 더미데이터 생성 -> 개별 save() 사용 성능 테스트")
	void testIndividualSavePerformance() {
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < THREAD_COUNT; i++) {
			User user = User.builder()
					.loginId("loginId" + i)
					.name("name" + i)
					.password("password" + i)
					.build();
			userRepository.save(user);
		}

		userRepository.flush(); // ID 할당 보장
		long endTime = System.currentTimeMillis();

		log.info("개별 save() 실행 시간: {} ms", (endTime - startTime));
		assertThat(userRepository.count()).isEqualTo(THREAD_COUNT);
	}

	@Test
	@DisplayName("user 더미데이터 생성 ->  saveAll() 사용 성능 테스트")
	void testSaveAllPerformance() {
		long startTime = System.currentTimeMillis();

		List<User> users = new ArrayList<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			users.add(User.builder()
					.loginId("loginId" + i)
					.name("name" + i)
					.password("password" + i)
					.build());
		}

		userRepository.saveAll(users);
		userRepository.flush(); // ID 할당 보장
		long endTime = System.currentTimeMillis();

		log.info("saveAll() 실행 시간: {} ms", (endTime - startTime));
		assertThat(userRepository.count()).isEqualTo(THREAD_COUNT);
	}
}