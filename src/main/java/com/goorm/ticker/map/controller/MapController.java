package com.goorm.ticker.map.controller;

import com.goorm.ticker.map.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name="Maps", description = "지도 SSE 연결")
@RestController
@RequiredArgsConstructor
@RequestMapping("/maps")
public class MapController {
    private final MapService mapService;

    @Operation(summary = "지도 SSE 연결", description = "url 파라미터에 유저의 id, 실시간으로 조회하고 싶은 음식점의 정보들을 쿼리 파라미터에 넣어 (음식점의 아이디들,x 좌표들, y좌표들, 이름들)리스트 형식으로 보내주세요. ex)restaurantId=1,2,3,4 & x=1,2,3,4 & y=1,2,3,4 & name = 음식점1,음식점2,음식점3,음식점4")
    @GetMapping(value = "/{userId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sub(@PathVariable Long userId, @RequestParam List<Long> restaurantId, @RequestParam List<String> x, @RequestParam List<String> y, @RequestParam List<String> name){
        return mapService.addEmitter(userId,x,y,restaurantId,name);
    }

}
