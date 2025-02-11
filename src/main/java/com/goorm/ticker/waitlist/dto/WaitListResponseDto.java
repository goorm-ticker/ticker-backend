package com.goorm.ticker.waitlist.dto;

import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;

public record WaitListResponseDto(
        Long waitingId,
        Long userId,
        Long restaurantId,
        Integer waitingNumber,
        Status status
) {
    public WaitListResponseDto(WaitList waitList) {
        this(
                waitList.getWaitingId(),
                waitList.getUser().getId(),
                waitList.getRestaurant().getRestaurantId(),
                waitList.getWaitingNumber(),
                waitList.getStatus()
        );
    }
}
