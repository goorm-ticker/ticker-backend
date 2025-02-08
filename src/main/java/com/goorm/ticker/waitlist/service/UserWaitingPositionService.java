package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserWaitingPositionService {

    private final WaitListRepository waitListRepository;
    private final HttpSession httpSession;

    public int getUserWaitingPosition(Long restaurantId) {
        // 세션에서 사용자 ID 가져오기
        Long userId = getSessionUserId();

        // 현재 대기 중인 식당 및 대기번호 조회
        WaitList userWaitList = findUserWaitingList(restaurantId, userId);

        // 앞의 대기 중인 사용자 수 반환
        return countUserAhead(userWaitList);
    }

    private Long getSessionUserId() {
        Long userId = (Long) httpSession.getAttribute("user");
        if (userId == null)
            throw new CustomException(ErrorCode.SESSION_EXPIRED);

        return userId;
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
}
