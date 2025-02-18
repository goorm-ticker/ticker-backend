package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.waitlist.dto.WaitListRequestDto;
import com.goorm.ticker.waitlist.dto.WaitListResponseDto;
import com.goorm.ticker.waitlist.dto.WaitingInfoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitListServiceImpl implements WaitListService {

    private final RegisterWaitingService registerWaitingService;
    private final CompleteWaitingService completeWaitingService;
    private final CancelWaitingService cancelWaitingService;
    private final WaitingPositionService waitingPositionService;

    @Override
    public WaitListResponseDto registerWaiting(WaitListRequestDto requestDto, Long userId) {
        return registerWaitingService.registerWaiting(requestDto, userId);
    }

    @Override
    public void completeWaiting(Long restaurantId) {
        completeWaitingService.completeWaiting(restaurantId);
    }

    @Override
    public void cancelWaiting(Long restaurantId) {
        cancelWaitingService.cancelWaiting(restaurantId);
    }

    @Override
    public WaitingInfoResponseDto getUserWaitingPosition(Long restaurantId, Long userId) {
        return waitingPositionService.getUserWaitingPosition(restaurantId, userId);
    }

    @Override
    public long getTotalWaitingCount(Long restaurantId) {
        return waitingPositionService.getTotalWaitingCount(restaurantId);
    }

}
