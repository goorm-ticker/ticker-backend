package com.goorm.ticker.reservation;

import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationStatusPublisher reservationStatusPublisher;

    @Scheduled(fixedRate = 60000)
    public void cancelReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(1);

        log.info("예약 자동 취소 스케줄러 실행: 현재 시간={}, 기준 시간={}", now, threshold);

        List<Reservation> expiredReservations =
                reservationRepository.findByReservationDateTimeBeforeAndStatusNot(threshold, ReservationStatus.ENTERED);

        if (expiredReservations.isEmpty()) {
            log.info("자동 취소 대상 예약 없음");
        } else {
            for (Reservation reservation : expiredReservations) {
                log.info("자동 취소 대상 예약: reservationId={}, status={}", reservation.getReservationId(), reservation.getStatus());

                reservation.cancelReservation();
                reservationRepository.save(reservation);

                log.info("예약 자동 취소 완료: reservationId={}", reservation.getReservationId());

                reservationStatusPublisher.publishReservationStatus(reservation.getReservationId(), "CANCELLED");
            }
        }
    }
}
