package com.goorm.ticker.notification.subscriber;

import com.goorm.ticker.notification.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ReservationStatusSubscriber implements MessageListener {
    private final FCMService fcmService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String reservationStatus = new String(message.getBody(), StandardCharsets.UTF_8);
        System.out.println("Received reservation status update: " + reservationStatus);

        String title = "예약 상태 업데이트";
        String body = "예약 상태가 변경되었습니다: " + reservationStatus;
        fcmService.sentNotification("general", title, body);
    }

}
