package com.goorm.ticker.notification.service;

import com.goorm.ticker.notification.dto.NotificationRequest;
import com.goorm.ticker.notification.dto.NotificationResponse;
import com.goorm.ticker.notification.entity.Notification;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FCMService fcmService;
    private final ReservationStatusPublisher reservationStatusPublisher;

    @Transactional
    public void createNotification(NotificationRequest request) {
        log.info("알림 생성 시작");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Notification notification = Notification.createNotification(
                user,
                request.getMessage(),
                request.getType()
        );
        notificationRepository.save(notification);

        log.info("알림 타입: {}", request.getType());

        if (request.getType() == NotificationType.RESERVATION_CONFIRMATION) {
            log.info("예약 확정 알림 전송 중...");
            String title = "예약이 확정되었습니다.";
            String body = "예약이 성공적으로 확정되었습니다. 입장 시간이 되면 방문해주세요.";
            fcmService.sentNotification("general", title, body);
        } else if (request.getType() == NotificationType.WAITLIST_NUMBER_CHANGE) {
            log.info("대기 순번 변경 알림 전송 중...");
            String title = "대기 순번 변경";
            String body = String.format("대기 순번이 %s 번으로 변경되었습니다.", request.getMessage());
            fcmService.sentNotification("general", title, body);
            reservationStatusPublisher.publishReservationStatus(title + ": " + body);
        } else {
            log.warn("알 수 없는 알림 타입: {}", request.getType());
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void sendEntryPossibleNotifications() {
        log.info("입장 가능 알림 전송");
        List<Notification> notifications = notificationRepository.findByTypeAndMessage(NotificationType.WAITLIST_NUMBER_CHANGE, "1");

        for (Notification notification : notifications) {
            User user = notification.getUser();

            boolean alreadyNotified = notificationRepository.existsByUserAndType(user, NotificationType.ENTRY_POSSIBLE);
            if (alreadyNotified) {
                log.info("입장 가능 알림 수신");
                continue;
            }

            Notification entryNotification = Notification.createNotification(
                    user,
                    "입장이 가능합니다. 매장으로 와주세요!",
                    NotificationType.ENTRY_POSSIBLE
            );
            notificationRepository.save(entryNotification);

            String title = "입장 가능 알림";
            String body = "입장 시간이 되었습니다. 매장으로 와주세요!";
            fcmService.sentNotification("general", title, body);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getUser().getId(),
                        notification.getMessage(),
                        notification.getType(),
                        notification.isRead(),
                        notification.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 알림을 찾을 수 없습니다."));
        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}