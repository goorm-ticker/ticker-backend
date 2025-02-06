package com.goorm.ticker.user.dto;

import com.goorm.ticker.user.entity.User;
import lombok.Builder;

@Builder
public record UserResponseDto(
	Long id,
	String loginId,
	String name
) {

	public static UserResponseDto of(User user) {
		return UserResponseDto.builder()
			.id(user.getId())
			.loginId(user.getLoginId())
			.name(user.getName())
			.build();
	}
}
