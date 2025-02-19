package com.goorm.ticker.notification.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.service.NotificationService;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import com.goorm.ticker.waitlist.service.CompleteWaitingService;
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
    private final CompleteWaitingService completeWaitingService;

    @Override
    public synchronized void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);

        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(messageBody);

            if (jsonNode == null) {
                log.error("JSON 변환 실패");
                return;
            }
        } catch (Exception e) {
            log.error("JSON 파싱 오류 발생");
            return;
        }

        if (!jsonNode.hasNonNull("reservationId") || !jsonNode.hasNonNull("status")) {
            log.error("필수 필드 누락");
            return;
        }

        Long reservationId = jsonNode.get("reservationId").asLong();
        String status = jsonNode.get("status").asText();

        processReservationStatusChange(reservationId, status);
    }

    private void processReservationStatusChange(Long reservationId, String status) {
        try {
            Thread.sleep(500); // DB 반영 대기

            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> {
                        return new EntityNotFoundException("예약을 찾을 수 없습니다.");
                    });

            switch (status) {
                case "CANCELLED":
                    notificationService.sendReservationNotification(reservationId, NotificationType.RESERVATION_CANCEL);
                    break;
                case "CONFIRMED":
                    notificationService.sendReservationNotification(reservationId, NotificationType.RESERVATION_CONFIRMATION);
                    break;
                case "ENTERED":
                    handleEnteredReservation(reservation);
                    break;
                default:
                    log.warn("알 수 없는 예약 상태: {}", status);
            }

            reservation.updateLastNotificationStatus(status);
            reservationRepository.save(reservation);

            log.info("상태 업데이트 완료: reservationId={}, newStatus={}", reservationId, status);
        } catch (Exception e) {
            log.error("예약 상태 변경 메시지 처리 오류");
        }
    }

    private void handleEnteredReservation(Reservation reservation) {
        try {
            Long userId = reservation.getUser().getId();

            completeWaitingService.completeWaiting(userId);

            notificationService.sendEntryPossibleNotification(userId);
        } catch (Exception e) {
            log.error("오류 발생");
        }
    }
}
