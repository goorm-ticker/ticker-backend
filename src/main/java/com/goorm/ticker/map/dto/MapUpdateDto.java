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
    private Long waitingTime;
    private String placeUrl;

    @Builder
    public MapUpdateDto(Long restaurantId, String restaurantName, String x, String y, Long waiting, String placeUrl) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.x = x;
        this.y = y;
        this.waiting = waiting;
        this.placeUrl = placeUrl;
    }

    public void setMyWaiting(int myWaiting, int waitingTime){
        this.myWaiting = (long) myWaiting;
        this.waitingTime = (long) waitingTime;
    }

}
