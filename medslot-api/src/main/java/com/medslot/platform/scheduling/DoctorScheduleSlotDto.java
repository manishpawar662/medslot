package com.medslot.platform.scheduling;

import java.time.Instant;
import java.util.UUID;

public record DoctorScheduleSlotDto(
        UUID slotId,
        Instant startTime,
        Instant endTime,
        SlotStatus status,
        String clinicName
) {
    public static DoctorScheduleSlotDto from(TimeSlot slot) {
        return new DoctorScheduleSlotDto(
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus(),
                slot.getClinic().getName()
        );
    }
}