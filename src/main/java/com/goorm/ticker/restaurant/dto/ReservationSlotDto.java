package com.goorm.ticker.restaurant.dto;

import java.time.LocalTime;

import com.goorm.ticker.restaurant.entity.ReservationSlot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationSlotDto {
	private Long slotId;
	private LocalTime startTime;

	public static ReservationSlotDto fromEntity(ReservationSlot slot) {
		return new ReservationSlotDto(slot.getId(), slot.getSlotTime());
	}
}
