package com.goorm.ticker.notification.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationStatusPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;

    public void publishReservationStatus(Long reservationId, String status) {
        if (reservationId == null) {
            log.error("예약 상태 변경 실패: reservationId가 null입니다. 알림을 전송하지 않습니다.");
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("reservationId", reservationId);
            message.put("status", status);

            String jsonMessage = new ObjectMapper().writeValueAsString(message);
            redisTemplate.convertAndSend(topic.getTopic(), jsonMessage);

            log.info("예약 상태 변경: {}", jsonMessage);
        } catch (Exception e) {
            log.error("예약 상태 변경 실패: {}", e.getMessage());
        }
    }
}
