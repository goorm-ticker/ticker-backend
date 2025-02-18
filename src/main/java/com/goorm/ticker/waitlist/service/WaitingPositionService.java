package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.waitlist.dto.WaitingInfoResponseDto;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WaitingPositionService {

    // 대기번호당 20분
    private static final int WAIT_TIME_PER_PERSON = 20;

    private final WaitListRepository waitListRepository;

    public WaitingInfoResponseDto getUserWaitingPosition(Long restaurantId, Long userId) {
        WaitList userWaitList;
        int usersAhead;
        int estimatedWaitTime;
        // 현재 대기 중인 식당 및 대기번호 조회
        try {
            userWaitList = findUserWaitingList(restaurantId, userId);

            // 앞에 대기 중인 사용자 수 계산
            usersAhead = countUserAhead(userWaitList);

            // 예상 대기시간 계산 (대기번호당 20분)
            estimatedWaitTime = usersAhead * WAIT_TIME_PER_PERSON;
        }
        catch (CustomException e){
            usersAhead = -1;
            estimatedWaitTime = -1;
        }
        // 앞의 대기 중인 사용자 수 반환
        return new WaitingInfoResponseDto(usersAhead, estimatedWaitTime);
    }

    public long getTotalWaitingCount(Long restaurantId) {
        // 식당에 총 대기중인 사용자 수 반환
        return waitListRepository.countTotalWaitingByRestaurantId(restaurantId);
    }

    private WaitList findUserWaitingList(Long restaurantId, Long userId) {
        return waitListRepository.findByRestaurant_RestaurantIdAndUser_IdAndStatus(
                        restaurantId, userId, Status.WAITING)
                .orElseThrow(() -> new CustomException(ErrorCode.WAITLIST_NOT_FOUND));
    }

    private int countUserAhead(WaitList userWaitList) {
        return (int) waitListRepository.countByRestaurant_RestaurantIdAndWaitingNumberLessThanAndStatus(
                userWaitList.getRestaurant().getRestaurantId(), userWaitList.getWaitingNumber(), Status.WAITING);
    }

    public WaitingInfoResponseDto updateMyWaitingInfo(Long restaurantId, Long userId) {
        List<WaitList> waitList = waitListRepository.findRestaurantWaitngList(restaurantId);

        Map<Long, Integer> waitingOrder = new HashMap<>();
        for(int i = 0 ; i < waitList.size() ; i++){
            waitingOrder.put(waitList.get(i).getUser().getId(), i + 1);
        }

        if(!waitingOrder.containsKey(userId)) {
            return new WaitingInfoResponseDto(-1, -1);
        }
        int order = waitingOrder.get(userId);
        return new WaitingInfoResponseDto(order, order * WAIT_TIME_PER_PERSON);
    }
}
