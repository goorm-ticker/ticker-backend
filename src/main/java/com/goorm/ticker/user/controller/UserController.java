package com.goorm.ticker.user.controller;

import com.goorm.ticker.user.dto.LoginRequestDto;
import com.goorm.ticker.user.dto.UserRegisterRequestDto;
import com.goorm.ticker.user.dto.UserResponseDto;
import com.goorm.ticker.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="Users", description = "회원 관리 API 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	@Operation(summary = "회원 가입", description = "바디에 {loginId, name, password} json 형식으로 보내주세요. 회원 가입 성공 시 데이터베이스 PK 값이 반환됩니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201",description = "회원가입 성공",content = @Content(schema = @Schema(implementation = Long.class))),
		@ApiResponse(responseCode = "400",description = "유효성 검사 실패",content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody @Valid UserRegisterRequestDto request) {
		UserResponseDto response = userService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "로그인", description = "바디에 {loginId, password} json 형식으로 보내주세요. 로그인 성공 시 세션에 사용자 PK 값이 추가됩니다..")
	@ApiResponses({
		@ApiResponse(responseCode = "200",description = "로그인 성공",content = @Content(schema = @Schema(implementation = Long.class))),
		@ApiResponse(responseCode = "400",description = "유효성 검사 실패",content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto request, HttpSession session) {
		Long userId = userService.login(request);
		session.setAttribute("user", userId);
		return ResponseEntity.ok("로그인 성공");
	}

	@Operation(summary = "로그아웃", description = "세션 유효한 경우만 로그아웃 가능")
	@ApiResponses({
		@ApiResponse(responseCode = "200",description = "로그아웃 성공",content = @Content(schema = @Schema(implementation = String.class))),
	})
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session){
		session.invalidate();
		return ResponseEntity.ok("로그아웃 성공");
	}

	@Operation(summary = "사용자 정보 가져오기", description = "세션 유효한 경우만 사용자 정보 확인 가능")
	@ApiResponses({
		@ApiResponse(responseCode = "200",description = "사용자 정보 가져오기 성공",content = @Content(schema = @Schema(implementation = String.class))),
		@ApiResponse(responseCode = "401",description = "세션 유효하지 않음",content = @Content(schema = @Schema(implementation = String.class)))
	})
	@GetMapping
	public ResponseEntity<?> findUserById(HttpSession session) {
		Long userId = (Long) session.getAttribute("user");
		return ResponseEntity.ok(userService.findUserById(userId));
	}
}
