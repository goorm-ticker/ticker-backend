package com.goorm.ticker.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.map.dto.MapUpdateDto;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.waitlist.dto.WaitingInfoResponseDto;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;

import com.goorm.ticker.waitlist.service.WaitingPositionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MapService {
    private final RestaurantRepository restaurantRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>(); //연결된 유저를 저장
    private final Map<Long, Set<Long>> viewer = new ConcurrentHashMap<>(); //식당 - 식당을 조회 중인 유저 리스트
    private final long timeout = 60 * 60 * 1000L;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WaitListRepository waitListRepository;
    private final WaitingPositionService waitingPositionService;


    /*
    SSE에 연결하는 메소드로 유저가 조회하는 식당의 실시간 대기열과 유저의 대기열을 보냅니다.
     */
    @Transactional
    public SseEmitter addEmitter(Long userId, List<String> x, List<String> y, List<Long> restaurantId, List<String> name){
        SseEmitter emitter = new SseEmitter(timeout);
        /*
        각 식당이 db에 존재하는지 확인, 없다면 식당을 db에 추가
         */
        saveRestaurants(x, y, restaurantId, name);
        List<MapUpdateDto> dto = waitListRepository.findRestaurantsWithWaiting(restaurantId);
        log.info("{}",name);
        for(Long rest : restaurantId){
            viewer.computeIfAbsent(rest, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }

        /*
        SSE 연결 유저 정보 저장
         */
        emitters.put(userId,emitter);
        /*
        단일 식당 조회 시, 현재 조회 중인 음식점들의 대기열과 본인의 대기열을 제공
         */
        if(restaurantId.size()==1){
             WaitingInfoResponseDto waitingInfoResponseDto = waitingPositionService.getUserWaitingPosition(restaurantId.get(0),userId);

            dto.get(0).setMyWaiting(waitingInfoResponseDto.waitingCount(),waitingInfoResponseDto.estimatedWaitTime());
        }
        log.info("음식점 등록 및 대기열 조회 성공");
        try {
            emitter.send(SseEmitter.event().name("connect").id(userId.toString()).data(objectMapper.writeValueAsString(dto)));
        }
        catch (Exception e){
            log.info(e.getMessage());
        }

        emitter.onCompletion(() -> {
            remove(userId);
            log.info("SSE 연결 종료 - 사용자 ID: {}", userId);
        });
        emitter.onTimeout(() -> {
            remove(userId);
            log.info("SSE 타임아웃 - 사용자 ID: {}", userId);
        });
        emitter.onError((e) -> {
            remove(userId);
            log.info("SSE 연결 에러 - 사용자 ID: {}, 에러: {}", userId, e.getMessage());
        });
        return  emitter;


    }

    // SSE 연결 수동 종료 메소드
    public void removeSSE(Long userId){
        SseEmitter emitter = emitters.remove(userId);
        emitter.complete();
    }

    // SSE 연결 종료 후 Map 에서 제거
    public void remove(Long userId) {
        viewer.forEach((rest, viewers) -> {
            viewers.remove(userId);
            if(viewers.isEmpty()){
                viewer.remove(rest);
            }
        });
        emitters.remove(userId);
        log.info("연결해제 id : {}, 현재 연결 인원 : {}, 조회 중인 식당 : {}",userId,emitters.size(),viewer.size());
    }



    public void saveRestaurants(List<String> x, List<String> y, List<Long> restaurantId, List<String> name){
        List<Long> ids = restaurantRepository.findExistingIds(restaurantId);
        List<Restaurant> restaurants = new ArrayList<>();
        for(int i = 0 ; i < restaurantId.size();i++){
            if(!ids.contains(restaurantId.get(i))){
                restaurants.add(Restaurant.builder().restaurantId(restaurantId.get(i)).x(x.get(i)).y(y.get(i)).restaurantName(name.get(i)).maxWaiting(Integer.MAX_VALUE).build());
            }
        }
        restaurantRepository.saveAll(restaurants);
    }


    //대기열 업데이트를 트리거로 해당 식당을 조회 중인 사용자 업데이트된 실시간 대기열과 본인의 대기열을 전달
    public void updateMap(Long restaurantId){
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(()->new CustomException(ErrorCode.RESTAURANT_NOT_FOUND));
        long waiting = waitListRepository.countTotalWaitingByRestaurantId(restaurantId);
        MapUpdateDto mapUpdateDto = MapUpdateDto.builder()
                .restaurantId(restaurantId)
                .restaurantName(restaurant.getRestaurantName())
                .x(restaurant.getX())
                .y(restaurant.getY())
                .waiting(waiting)
                .build();
        List<WaitList> waitList = waitListRepository.findRestaurantWaitingList(restaurantId);
        Map<Long,Integer> wait = new HashMap<>();
        for(int i = 0 ; i < waitList.size() ; i++){
            Long userId = waitList.get(i).getUser().getId();
            wait.put(userId,i+1);
        }
        for(Long user : viewer.get(restaurantId)){
            try {
                int count = wait.getOrDefault(user,0);
                mapUpdateDto.setMyWaiting(count,count * 20);
                emitters.get(user).send(SseEmitter.event().name("update").id(user.toString()).data(objectMapper.writeValueAsString(mapUpdateDto)));
            }
            catch (Exception e){
                log.info(e.getMessage());
            }
        }
    }
}
