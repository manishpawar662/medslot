package com.medslot.platform.scheduling;

import java.time.Instant;
import java.util.UUID;

public record TimeSlotResponseDto(
        UUID id,
        UUID doctorId,
        String doctorName,
        UUID clinicId,
        Instant startTime,
        Instant endTime,
        SlotStatus status
) {
    public static TimeSlotResponseDto from(TimeSlot slot) {
        return new TimeSlotResponseDto(
                slot.getId(),
                slot.getDoctor().getId(),
                slot.getDoctor().getUser().getFullName(),
                slot.getClinic().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}