package com.goorm.ticker.waitlist.controller;

import com.goorm.ticker.waitlist.dto.WaitListRequestDto;
import com.goorm.ticker.waitlist.dto.WaitListResponseDto;
import com.goorm.ticker.waitlist.dto.WaitingInfoResponseDto;
import com.goorm.ticker.waitlist.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="WaitList", description = "대기열 API 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/waitlist")
public class WaitListController {

    private final RegisterWaitingService registerUserService;
    private final CompleteWaitingService completeWaitingService;
    private final CancelWaitingService cancelWaitingService;
    private final WaitingPositionService waitingPositionService;

    @Operation(summary = "대기 등록", description = "사용자를 대기열에 등록합니다.")
    @PostMapping
    public ResponseEntity<WaitListResponseDto> registerWaiting(@RequestBody WaitListRequestDto request) {
        WaitListResponseDto response = registerUserService.registerWaiting(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대기 순번 조회", description = "사용자의 현재 대기 순번과 예상 대기 시간을 반환합니다.")
    @GetMapping("/{restaurantId}/position")
    public ResponseEntity<WaitingInfoResponseDto> getUserWaitingPosition(
            @Parameter(description = "조회할 식당 ID", required = true)
            @PathVariable("restaurantId") Long restaurantId) {
        WaitingInfoResponseDto waitingInfo = waitingPositionService.getUserWaitingPosition(restaurantId);
        return ResponseEntity.ok(waitingInfo);
    }

    @Operation(summary = "총 대기 인원 조회", description = "식당에 현재 대기 인원을 반환합니다.")
    @GetMapping("/{restaurantId}/totalWaiting")
    public ResponseEntity<Long> getTotalWaiting(
            @Parameter(description = "조회할 식당 ID", required = true)
            @PathVariable("restaurantId") Long restaurantId) {
        long totalWaiting = waitingPositionService.getTotalWaitingCount(restaurantId);
        return ResponseEntity.ok(totalWaiting);
    }

    @Operation(summary = "입장 완료", description = "사용자의 대기 상태를 '입장 완료'로 변경합니다.")
    @PatchMapping("/complete")
    public ResponseEntity<?> completeWaiting() {
        completeWaitingService.completeWaiting();
        return ResponseEntity.ok("식당 입장 완료되었습니다.");
    }

    @Operation(summary = "대기 취소", description = "사용자의 대기열 상태를 '취소'로 변경합니다.")
    @PatchMapping("/cancel")
    public ResponseEntity<?> cancelWaiting() {
        cancelWaitingService.cancelWaiting();
        return ResponseEntity.ok("대기열이 취소되었습니다.");
    }

    /* 대기열 리스트 (사용 안 하면 삭제 예정입니다.)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<WaitListResponseDto>> getWaitingListStream() {
        return Flux.interval(Duration.ofSeconds(3))
                .map(sequence -> waitingListService.getWaitingList());
    }*/

    /* 식당 대기열 목록 (사용 안 하면 삭제 예정입니다.)
    @GetMapping(value = "/{restaurantId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<WaitListResponseDto>> getRestaurantWaitingListStream(@PathVariable Long restaurantId) {
        return Flux.interval(Duration.ofSeconds(3))
                .map(sequence -> restaurantWaitingService.getWaitingListByRestaurant(restaurantId));
    }*/
}
