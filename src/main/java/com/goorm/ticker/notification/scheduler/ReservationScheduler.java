package com.goorm.ticker.notification.scheduler;

import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationStatusPublisher reservationStatusPublisher;
    private final ReservationService reservationService;

    @Async
    @Scheduled(fixedRate = 300000)
    public void cancelReservations() {
        LocalTime now = LocalTime.now();

        log.info("예약 자동 취소 스케줄러 실행: 현재 시간={}", now);

        List<Reservation> expiredReservations = reservationRepository.findPendingReservationsPastSlotTime(now.minusMinutes(5));

        if (expiredReservations.isEmpty()) {
            log.info("자동 취소 대상 예약 없음");
        } else {
            for (Reservation reservation : expiredReservations) {
                log.info("자동 취소 대상 예약: reservationId={}, status={}", reservation.getReservationId(), reservation.getStatus());

                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    log.info("이미 취소된 예약: reservationId={}", reservation.getReservationId());
                    continue;
                }

                if (!reservation.isNotificationAlreadySent("CANCELLED")) {
                    reservation.cancelReservation();
                    reservation.updateLastNotificationStatus("CANCELLED");
                    reservationRepository.save(reservation);
                    reservationService.sendNotificationIfNeeded(reservation, "CANCELLED");
                }
            }
        }
    }
}
