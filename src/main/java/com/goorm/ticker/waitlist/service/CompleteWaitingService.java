package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
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
    private final HttpSession httpSession;

    @Transactional
    public void completeWaiting() {
        // 세션에서 사용자 ID 가져오기
        Long userId = getSessionUserId();

        // 사용자 대기열의 정보 조회
        WaitList waitList = findUserWaitingList(userId);

        waitList.updateStatus(Status.ENTERED);
    }

    private Long getSessionUserId() {
        Long userId = (Long) httpSession.getAttribute("user");
        if (userId == null)
            throw new CustomException(ErrorCode.SESSION_EXPIRED);

        return userId;
    }

    private WaitList findUserWaitingList(Long userId) {
        return waitListRepository.findByUser_IdAndStatus(userId, Status.WAITING)
                .orElseThrow(() -> new CustomException(ErrorCode.WAITLIST_NOT_FOUND));
    }
}
