package com.goorm.ticker.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class MapUpdateDto {
    private Long restaurantId;
    private String restaurantName;
    private String x;
    private String y;
    private Long waiting;

    public MapUpdateDto(Long restaurantId, String restaurantName, String x, String y, Long waiting) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.x = x;
        this.y = y;
        this.waiting = waiting;
    }

}
