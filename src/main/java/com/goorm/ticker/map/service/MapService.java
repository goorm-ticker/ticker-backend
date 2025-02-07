package com.goorm.ticker.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.map.dto.MapUpdateDto;
import com.goorm.ticker.map.util.RestaurantEmitter;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MapService {
    private final RestaurantRepository restaurantRepository;

    private final Map<Long, RestaurantEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> viewer = new ConcurrentHashMap<>();
    private final long timeout = 60 * 60 * 1000L;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /*
    SSE에 연결하는 메소드로 해당 식당의 정보와 접속 중인 유저의 id를 받아 유저의 id를 키로, 현재 조회 중인 식당의 정보를 저장하고 연결합니다.
     */
    @Transactional
    public SseEmitter addEmitter(Long userId, List<String> x, List<String> y, List<Long> restaurantId, List<String> name){
        SseEmitter emitter = new SseEmitter(timeout);
        List<MapUpdateDto> dto = new ArrayList<>();
        List<Restaurant> restaurants = new ArrayList<>();
        List<Long> ids = restaurantRepository.findExistingIds(restaurantId);
        /*
        각 식당이 db에 존재하는지 확인, 없다면 식당을 db에 추가
         */
        for(int i = 0 ; i < restaurantId.size();i++){
            if(!ids.contains(restaurantId.get(i))){
                restaurants.add(Restaurant.builder().restaurantId(restaurantId.get(i)).x(x.get(i)).y(y.get(i)).restaurantName(name.get(i)).maxWaiting(Integer.MAX_VALUE).build());
            }
            /*
            해당 식당의 대기열 수를 조회
            */
            MapUpdateDto mapUpdateDto = MapUpdateDto.builder().restaurantId(restaurantId.get(i)).x(x.get(i)).y(y.get(i)).name(name.get(i)).waiting(0/*조회된 대기열*/).build();
            dto.add(mapUpdateDto);
        }
        restaurantRepository.saveAll(restaurants);
         // 2번 케이스에 대한 방법입니다.
        /*for(Long rest : restaurantId){
            viewer.computeIfAbsent(rest, k -> new CopyOnWriteArrayList<>()).add(userId);
        }*/

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
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        return  emitter;
    }


    //2번 케이스의 삭제 로직
    /*public void remove(Long userId){
        viewer.forEach((rest, viewers)->{
            viewers.remove(userId);
        });
        emitters.remove(userId);
    }*/
    /*
        식당의 대기열에 변화가 생기면 해당 식당의 id와 변경된 대기열을 현재 그 식당을 조회하고 있는 회원들에게 변동된 대기열에 대해 알림을 보냅니다.
        개선 방향
        1. 현재 방식 유지 (현재 조회 중인 모든 사람들의 음식점을 풀스캔) - 느림
        2. 식당 관련 맵을 하나 더 만들어서 관리 ( 맵 규모 2배로 커짐 ) - 매우 빠름
    */
    public void updateMap(Long restaurantId, int waiting){
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(()->new CustomException(ErrorCode.NOT_FOUND_RESTAURANT));
        MapUpdateDto mapUpdateDto = MapUpdateDto.builder().restaurantId(restaurantId).x(restaurant.getX()).y(restaurant.getY()).name(restaurant.getRestaurantName()).waiting(waiting).build();
        /*
        갱신된 대기열에 대한 정보를 해당 음식점을 조회 중인 사용자를 전부 찾아 전달
         */
        emitters.forEach((userId,restaurantEmitter)->{
            if(restaurantEmitter.getRestaurantId().contains(restaurantId)){
                try {
                    restaurantEmitter.getEmitter().send(SseEmitter.event().name("update").id(userId.toString()).data(objectMapper.writeValueAsString(mapUpdateDto)));
                }
                catch (Exception e){
                    log.info(e.getMessage());
                }
            }
        });

        /*
            2번 케이스에 대한 방법입니다.
        for(Long user : viewer.get(restaurantId)){
            try {
                emitters.get(user).getEmitter().send(SseEmitter.event().name("update").id(user.toString()).data(objectMapper.writeValueAsString(mapUpdateDto)));
            }
            catch (Exception e){
                log.info(e.getMessage());
            }
        }

         */
    }
}
