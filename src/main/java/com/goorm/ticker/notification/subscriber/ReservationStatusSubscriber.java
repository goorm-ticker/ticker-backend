package com.goorm.ticker.notification.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.service.NotificationService;
import com.goorm.ticker.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationStatusSubscriber implements MessageListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ReservationService reservationService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("예약 상태 변경 감지: {}", messageBody);

        try {
            JsonNode jsonNode = objectMapper.readTree(messageBody);

            if (jsonNode.get("reservationId") == null) {
                log.error("reservationId가 null입니다.");
                return;
            }

            Long reservationId = jsonNode.get("reservationId").asLong();
            String status = jsonNode.get("status").asText();

            if (!reservationService.existsById(reservationId)) {
                log.error("reservionId={} 예약이 존재하지 않습니다.");
                return;
            }

            if (isMessageFromSchedulerOrAPI(reservationId, status)) {
                switch (status) {
                    case "CANCELLED" -> {
                        log.info("예약 취소 감지: reservationId={}", reservationId);
                        notificationService.sendReservationNotification(reservationId, NotificationType.RESERVATION_CANCEL);
                    }
                    case "CONFIRMED" -> {
                        log.info("예약 확정 감지: reservationId={}", reservationId);
                        notificationService.sendReservationNotification(reservationId, NotificationType.RESERVATION_CONFIRMATION);
                    }
                    default -> log.warn("알 수 없는 예약 상태: {}", status);
                }
            } else {
                log.info("이미 처리된 메시지입니다.");
            }
        } catch (Exception e) {
            log.error("예약 상태 변경 메시지 처리 오류: {}", e.getMessage());
        }
    }
    private boolean isMessageFromSchedulerOrAPI(Long reservationId, String status) {
        return reservationService.isStatusAlreadyUpdated(reservationId, status);
    }
}
