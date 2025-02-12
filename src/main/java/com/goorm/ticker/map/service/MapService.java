package com.goorm.ticker.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.map.dto.MapUpdateDto;
import com.goorm.ticker.map.util.RestaurantEmitter;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.waitlist.repository.WaitListRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MapService {
    private final RestaurantRepository restaurantRepository;
    private final Map<Long, RestaurantEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> viewer = new ConcurrentHashMap<>();
    private final long timeout = 60 * 60 * 1000L;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WaitListRepository waitListRepository;


    /*
    SSE에 연결하는 메소드로 해당 식당의 정보와 접속 중인 유저의 id를 받아 유저의 id를 키로, 현재 조회 중인 식당의 정보를 저장하고 연결합니다.
     */
    @Transactional
    public SseEmitter addEmitter(Long userId, List<String> x, List<String> y, List<Long> restaurantId, List<String> name){
        SseEmitter emitter = new SseEmitter(timeout);
        /*
        각 식당이 db에 존재하는지 확인, 없다면 식당을 db에 추가
         */
        saveRestaurants( x, y, restaurantId, name);
        List<MapUpdateDto> dto = waitListRepository.findRestaurantsWithWaiting(restaurantId);
        for(Long rest : restaurantId){
            viewer.computeIfAbsent(rest, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }

        /*
        현재 접속한 사용자와 사용자가 실시간 대기열 조회를 하는 음식점을 저장
         */
        RestaurantEmitter restaurantEmitter = RestaurantEmitter.builder().emitter(emitter).restaurantId(restaurantId).build();
        emitters.put(userId,restaurantEmitter);
        /*
        현재 조회 중인 음식점들의 대기열을 제공
         */
        try {
            emitter.send(SseEmitter.event().name("connect").id(userId.toString()).data(objectMapper.writeValueAsString(dto)));
        }
        catch (Exception e){
            log.info(e.getMessage());
        }

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            remove(userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            remove(userId);
        });
        return  emitter;

    }

    @Transactional
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

    public void remove(Long userId){
        viewer.forEach((rest, viewers)->{
            viewers.remove(userId);
        });
        emitters.remove(userId);
    }

    //대기열 업데이트를 트리거로 해당 식당을 조회 중인 사용자에게 전달
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
        for(Long user : viewer.get(restaurantId)){
            try {
                emitters.get(user).getEmitter().send(SseEmitter.event().name("update").id(user.toString()).data(objectMapper.writeValueAsString(mapUpdateDto)));
            }
            catch (Exception e){
                log.info(e.getMessage());
            }
        }

    }
}
