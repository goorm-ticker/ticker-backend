package com.goorm.ticker.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserRegisterRequestDto (
	@NotBlank(message = "로그인 아이디는 필수입니다.") String loginId,
	@NotBlank(message = "사용자 이름은 필수입니다.") String name,
	@NotBlank(message = "비밀번호는 필수입니다.") String password
) {
}
