package com.goorm.ticker.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequestDto(
	@NotBlank String loginId,
	@NotBlank String password
) {
}
