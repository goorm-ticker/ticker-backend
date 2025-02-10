package com.goorm.ticker.map.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RestaurantEmitter {
    private SseEmitter emitter;
    private List<Long> restaurantId;
}
