/*
package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.waitlist.dto.WaitListResponseDto;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaitingListService {

    private final WaitListRepository waitListRepository;

    public List<WaitListResponseDto> getWaitingList() {
        return waitListRepository.findByStatus(Status.WAITING)
                .stream()
                .map(WaitListResponseDto::new)
                .collect(Collectors.toList());
    }
}
*/
