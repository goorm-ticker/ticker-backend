package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.waitlist.dto.WaitListRequestDto;
import com.goorm.ticker.waitlist.dto.WaitListResponseDto;
import com.goorm.ticker.waitlist.dto.WaitingInfoResponseDto;

public interface WaitListService {
    WaitListResponseDto registerWaiting(WaitListRequestDto requestDto, Long userId);
    void completeWaiting(Long restaurantId);
    void cancelWaiting(Long restaurantId);
    WaitingInfoResponseDto getUserWaitingPosition(Long restaurantId, Long userId);
    long getTotalWaitingCount(Long restaurantId);
}
