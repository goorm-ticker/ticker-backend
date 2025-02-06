package com.goorm.ticker.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.user.dto.LoginRequestDto;
import com.goorm.ticker.user.dto.UserRegisterRequestDto;
import com.goorm.ticker.user.repository.UserRepository;
import com.goorm.ticker.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@AfterEach
	void afterEach() {
		userRepository.deleteAll();
	}

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private UserService userService;

	@Test
	@DisplayName("회원가입 성공 -> Created 상태 코드 반환")
	public void registerSucceedTest() throws Exception {
		// setup
		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto request = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		String body = objectMapper.writeValueAsString(request);

		// run & verify
		mockMvc
			.perform(post("/users/register").content(body).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.loginId").value(testLoginId))
			.andExpect(jsonPath("$.name").value(testName))
			.andDo(print());
	}

	@Test
	@DisplayName("회원 가입 사용자 이름 유효성 검사 실패 -> BadRequest 상태 코드 예외 발생")
	public void registerNameIsInvalidTest() throws Exception {
		// setup
		final String testName = " ";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto request = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword).
			build();

		String body = objectMapper.writeValueAsString(request);

		// run & verify
		mockMvc.perform(post("/users/register").content(body).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$").value("사용자 이름은 필수입니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("회원 가입 로그인 아이디 유효성 검사 실패 -> BadRequest 상태 코드 예외 발생")
	public void registerLoginIdIsInvalidTest() throws Exception {
		// setup
		final String testName = "testName";
		final String testLoginId = null;
		final String testPassword = "testPassword";

		UserRegisterRequestDto request = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword).
			build();

		String body = objectMapper.writeValueAsString(request);

		// run & verify
		mockMvc.perform(post("/users/register").content(body).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$").value("로그인 아이디는 필수입니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("로그인 성공 -> ok 상태 코드 반환")
	public void loginSucceedTest() throws Exception {
		// setup
		MockHttpSession session = new MockHttpSession();

		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto registerRequest = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		userService.register(registerRequest);

		LoginRequestDto loginRequest = LoginRequestDto.builder()
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		String body = objectMapper.writeValueAsString(loginRequest);

		// run & verify
		mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value("로그인 성공"))
			.andDo(print());
	}

	@Test
	@DisplayName("로그인 실패 -> Unauthorized 상태 코드 예외 발생")
	public void passwordIsIncorrectTest() throws Exception {
		// setup
		MockHttpSession session = new MockHttpSession();

		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto registerRequest = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		userService.register(registerRequest);

		LoginRequestDto loginRequest = LoginRequestDto.builder()
			.loginId(testLoginId)
			.password(testPassword + "1234")
			.build();

		String body = objectMapper.writeValueAsString(loginRequest);

		// run & verify
		mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$").value("비밀번호가 일치하지 않습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("세션 성공 -> 로그인 후 세션으로 자신의 정보 가져오기")
	public void sessionSucceedTest() throws Exception {
		// setup
		MockHttpSession session = new MockHttpSession();

		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto registerRequest = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		userService.register(registerRequest);

		LoginRequestDto loginRequest = LoginRequestDto.builder()
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		String body = objectMapper.writeValueAsString(loginRequest);

		// run & verify
		mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session));

		mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value(testName))
			.andExpect(jsonPath("$.loginId").value(testLoginId))
			.andDo(print());
	}

	@Test
	@DisplayName("로그아웃 후 자신의 정보 가져올 수 없음")
	public void logoutTest() throws Exception {
		// setup
		MockHttpSession session = new MockHttpSession();

		final String testName = "testName";
		final String testLoginId = "testLoginId";
		final String testPassword = "testPassword";

		UserRegisterRequestDto registerRequest = UserRegisterRequestDto.builder()
			.name(testName)
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		userService.register(registerRequest);

		LoginRequestDto loginRequest = LoginRequestDto.builder()
			.loginId(testLoginId)
			.password(testPassword)
			.build();

		String body = objectMapper.writeValueAsString(loginRequest);

		// run & verify
		mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session));

		mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value(testName))
			.andExpect(jsonPath("$.loginId").value(testLoginId))
			.andDo(print());

		mockMvc.perform(post("/users/logout").content(body).contentType(MediaType.APPLICATION_JSON).session(session));

		mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$").value("세션이 만료되었습니다."))
			.andDo(print());
	}
}