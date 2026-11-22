package com.medslot.platform.scheduling;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateSlotRequestDto(
        @NotNull(message = "doctorId is required")
        UUID doctorId,

        @NotNull(message = "startTime is required")
        @Future(message = "startTime must be in the future")
        Instant startTime,

        @NotNull(message = "endTime is required")
        @Future(message = "endTime must be in the future")
        Instant endTime
) {}