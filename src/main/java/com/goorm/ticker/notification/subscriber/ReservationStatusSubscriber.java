package com.goorm.ticker.notification.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.service.NotificationService;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
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
    private final ReservationRepository reservationRepository;

    @Override
    public synchronized void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("예약 상태 변경 감지: {}", messageBody);

        try {
            JsonNode jsonNode = objectMapper.readTree(messageBody);
            if (jsonNode.get("reservationId") == null) {
                log.error("reservationId가 null입니다. 메시지: {}", messageBody);
                return;
            }

            Long reservationId = jsonNode.get("reservationId").asLong();
            String status = jsonNode.get("status").asText();

            Thread.sleep(500); // DB 반영 대기

            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> {
                        log.error("예약을 찾을 수 없습니다. reservationId={}", reservationId);
                        return new EntityNotFoundException("예약을 찾을 수 없습니다.");
                    });

            log.info("🔎 기존 상태: {}, 새로운 상태: {}", reservation.getLastNotificationStatus(), status);

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

            reservation.updateLastNotificationStatus(status);
            reservationRepository.saveAndFlush(reservation);

            log.info("상태 업데이트 완료: reservationId={}, newStatus={}", reservationId, status);

        } catch (Exception e) {
            log.error("예약 상태 변경 메시지 처리 오류: {}", e.getMessage(), e);
        }
    }
}