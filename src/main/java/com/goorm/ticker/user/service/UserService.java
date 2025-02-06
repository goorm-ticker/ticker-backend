package com.goorm.ticker.user.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.user.dto.LoginRequestDto;
import com.goorm.ticker.user.dto.UserRegisterRequestDto;
import com.goorm.ticker.user.dto.UserResponseDto;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	public UserResponseDto register(UserRegisterRequestDto request) {
		if(userRepository.existsByLoginId(request.loginId())) {
			throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
		}

		try {
			User user = User.builder()
				.loginId(request.loginId())
				.name(request.name())
				.password(request.password())
				.build();
			userRepository.save(user);
			return UserResponseDto.of(user);
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
		}
	}

	public Long login(LoginRequestDto request) {
		User user = userRepository.findByLoginId(request.loginId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		if(!user.getPassword().equals(request.password())) {
			throw new CustomException(ErrorCode.LOGIN_FAIL);
		}
		return user.getId();
	}

	public UserResponseDto findUserById(Long id) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		return UserResponseDto.of(user);
	}
}
