package com.goorm.ticker.notification.service;

import com.goorm.ticker.notification.dto.NotificationRequest;
import com.goorm.ticker.notification.dto.NotificationResponse;
import com.goorm.ticker.notification.entity.Notification;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FCMService fcmService;
    private final ReservationStatusPublisher reservationStatusPublisher;
    private final ReservationRepository reservationRepository;
    private final WaitListRepository waitListRepository;

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

        if (request.getType() == NotificationType.RESERVATION_CONFIRMATION) {
            reservationStatusPublisher.publishReservationStatus(request.getReservationId(), "CONFIRMED");
            log.info("예약 상태 변경 이벤트 전송: reservationId={}, status=CONFIRMED", request.getReservationId());
        } else if (request.getType() == NotificationType.RESERVATION_CANCEL) {
            reservationStatusPublisher.publishReservationStatus(request.getReservationId(), "CANCELLED");
            log.info("예약 상태 변경 이벤트 전송: reservationId={}, status=CANCELLED", request.getReservationId());
        } else {
            log.warn("알 수 없는 알림 타입: {}", request.getType());
        }
    }

    @Transactional
    public void sendReservationNotification(Long reservationId, NotificationType notificationType) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

        User user = reservation.getUser();

        String title;
        String message;
        String status;

        switch (notificationType) {
            case RESERVATION_CONFIRMATION -> {
                title = "예약 확정 알림";
                message = "예약이 확정되었습니다. 방문 시간에 맞춰 방문해주세요.";
                status = "CONFIRMED";
            }
            case RESERVATION_CANCEL -> {
                title = "예약 취소 알림";
                message = "예약이 취소되었습니다.";
                status = "CANCELLED";
            }
            default -> {
                log.warn("알 수 없는 알림 타입: {}", notificationType);
                return;
            }
        }

        Notification notification = Notification.createNotification(user, message, notificationType);
        notificationRepository.save(notification);

        reservation.updateLastNotificationStatus(status);

        if (!reservation.isNotificationAlreadySent(status)) {
            fcmService.sentNotification("general", title, message);
        }
    }


    @Transactional
    public void sendEntryPossibleNotification(Long restaurantId) {
        log.info("식당 ID {} 대기 순번 1번 사용자에게 입장 가능 알림 전송 시도", restaurantId);

        Optional<WaitList> firstWaitingUser = waitListRepository.findRestaurantWaitingList(restaurantId)
                .stream()
                .filter(waitList -> waitList.getStatus() == Status.WAITING)
                .findFirst();

        if (firstWaitingUser.isEmpty()) {
            log.info("식당 ID {}에 대기자 없음", restaurantId);
            return;
        }

        WaitList waitList = firstWaitingUser.get();

        if (waitList.getStatus() == Status.ENTERED) {
            log.warn("이미 입장 처리된 사용자 (userId={}, restaurantId={})", waitList.getUser().getId(), restaurantId);
            return;
        }

        User user = waitList.getUser();

        String title = "입장 가능 알림";
        String message = "입장 순번이 되었습니다. 매장으로 와주세요";

        Notification notification = Notification.createNotification(user, message, NotificationType.ENTRY_POSSIBLE);
        notificationRepository.save(notification);

        fcmService.sentNotification("general", title, message);

        log.info("입장 가능 알림 전송 완료: userId={}, restaurantId={}", user.getId(), restaurantId);
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