package com.goorm.ticker.waitlist.dto;

import lombok.Builder;

@Builder
public record WaitListRequestDto(
        Long restaurantId
) { }
