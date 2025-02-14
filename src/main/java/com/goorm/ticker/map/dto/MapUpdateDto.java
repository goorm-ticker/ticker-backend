package com.goorm.ticker.map.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MapUpdateDto {
    private Long restaurantId;
    private String restaurantName;
    private String x;
    private String y;
    private Long waiting;
    private Long myWaiting;

    @Builder
    public MapUpdateDto(Long restaurantId, String restaurantName, String x, String y, Long waiting) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.x = x;
        this.y = y;
        this.waiting = waiting;
    }

    public void setMyWaiting(int myWaiting){
        this.myWaiting = (long) myWaiting;
    }

}
