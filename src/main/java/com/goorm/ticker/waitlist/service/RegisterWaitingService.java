package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.map.service.MapService;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import com.goorm.ticker.waitlist.dto.WaitListRequestDto;
import com.goorm.ticker.waitlist.dto.WaitListResponseDto;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterWaitingService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final WaitListRepository waitListRepository;
    private final HttpSession httpSession;
    private final MapService mapService;

    public WaitListResponseDto registerWaiting(WaitListRequestDto request) {
        // 세션에서 사용자 ID 가져오기
        Long userId = getSessionUserId();

        // 사용자 및 식당 정보 조회
        User user = findUserById(userId);
        Restaurant restaurant = findRestaurantById(request.restaurantId());

        // 동일한 식당에서 이미 대기 중인지 확인
        validateDuplicateWaiting(restaurant, user);

        // 새로운 대기 번호 할당
        int newWaitingNumber = getNextWaitingNumber(request);

        // 대기열 등록 및 저장
        WaitList waitList = WaitList.createWaitList(user, restaurant, newWaitingNumber);
        waitListRepository.save(waitList);

        //지도에서 해당 식당 대기열 업데이트
        mapService.updateMap(request.restaurantId());

        return new WaitListResponseDto(waitList);
    }

    private Long getSessionUserId() {
        Long userId = (Long) httpSession.getAttribute("user");
        if (userId == null)
            throw new CustomException(ErrorCode.SESSION_EXPIRED);

        return userId;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    private Restaurant findRestaurantById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESTAURANT_NOT_FOUND));
    }

    private void validateDuplicateWaiting(Restaurant restaurant, User user) {
        boolean alreadyExists = waitListRepository.existsByRestaurant_RestaurantIdAndUser_IdAndStatus(
                restaurant.getRestaurantId(), user.getId(), Status.WAITING
        );
        if (alreadyExists)
            throw new CustomException(ErrorCode.WAITLIST_ALREADY_EXISTS);
    }

    private int getNextWaitingNumber(WaitListRequestDto request) {
        Integer lastWaitingNumber = waitListRepository.findMaxWaitingNumberByRestaurantId(request.restaurantId());
        return (lastWaitingNumber == null) ? 1 : lastWaitingNumber + 1;
    }
}
