package com.goorm.ticker.user.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.user.dto.LoginRequestDto;
import com.goorm.ticker.user.dto.UserRegisterRequestDto;
import com.goorm.ticker.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@AfterEach
	void afterEach() {
		userRepository.deleteAll();
	}

	@DisplayName("이미 사용 중인 아이디로 회원가입 -> DUPLICATE_USERNAME 에러코드 예외 발생")
	@Test
	public void duplicatedLoginIdTest() {
		// setup
		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto request = UserRegisterRequestDto
			.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		userService.register(request);

		// run & verify
		Assertions.assertThatThrownBy(() -> userService.register(request))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.DUPLICATE_USERNAME, customException.getErrorCode());
				assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
				assertEquals("중복된 유저이름입니다.", customException.getErrorCode().getMessage());
			});
	}

	@DisplayName("비밀번호 불일치 -> LOGIN_FAIL 에러코드 예외를 던진다.")
	@Test
	public void passwordMatchTest() {
		// setup
		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto registerRequest = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		userService.register(registerRequest);

		LoginRequestDto loginRequestDto = LoginRequestDto.builder()
			.loginId(testLoginId)
			.password(testPassword + "1234")
			.build();

		Assertions.assertThatThrownBy(() -> userService.login(loginRequestDto))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.LOGIN_FAIL, customException.getErrorCode());
				assertEquals(HttpStatus.UNAUTHORIZED, customException.getErrorCode().getStatus());
				assertEquals("비밀번호가 일치하지 않습니다.", customException.getErrorCode().getMessage());
			});
	}
}