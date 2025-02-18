package com.goorm.ticker.notification.Scheduler;

import com.goorm.ticker.notification.service.NotificationService;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingListScheduler {

    private final WaitListRepository waitListRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000)
    public void checkAndSendEntryPossibleNotification() {

        List<Long> restaurantIds = waitListRepository.findAllDistinctRestaurantIds();

        for (Long restaurantId : restaurantIds) {
            log.info("식당 ID {}의 대기열 확인 중", restaurantId);

            notificationService.sendEntryPossibleNotification(restaurantId);
        }

        log.info("입장 가능 알림 전송 완료");
    }
}
