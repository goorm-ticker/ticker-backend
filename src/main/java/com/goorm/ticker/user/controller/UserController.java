package com.goorm.ticker.user.controller;

import com.goorm.ticker.user.dto.LoginRequestDto;
import com.goorm.ticker.user.dto.UserRegisterRequestDto;
import com.goorm.ticker.user.dto.UserResponseDto;
import com.goorm.ticker.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody @Valid UserRegisterRequestDto request) {
		UserResponseDto response = userService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto request, HttpSession session) {
		Long userId = userService.login(request);
		session.setAttribute("user", userId);
		return ResponseEntity.ok("로그인 성공");
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session){
		session.invalidate();
		return ResponseEntity.ok("로그아웃 성공");
	}

	@GetMapping
	public ResponseEntity<?> findUserById(HttpSession session) {
		Long userId = (Long) session.getAttribute("user");
		return ResponseEntity.ok(userService.findUserById(userId));
	}
}
