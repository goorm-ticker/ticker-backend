package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.map.service.MapService;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteWaitingService {

    private final WaitListRepository waitListRepository;
    private final MapService mapService;

    @Transactional
    public void completeWaiting(Long userId) {
        // 사용자 대기열의 정보 조회
        WaitList waitList = findUserWaitingList(userId);

        waitList.updateStatus(Status.ENTERED);

        //지도 대기열 업데이트
        mapService.updateMap(waitList.getRestaurant().getRestaurantId());
    }

    private WaitList findUserWaitingList(Long userId) {
        return waitListRepository.findByUser_IdAndStatus(userId, Status.WAITING)
                .orElseThrow(() -> new CustomException(ErrorCode.WAITLIST_NOT_FOUND));
    }
}
