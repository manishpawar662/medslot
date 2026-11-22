package com.medslot.platform.scheduling;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record BulkCreateSlotsRequestDto(
        @NotNull(message = "doctorId is required") UUID doctorId,
        @NotNull(message = "windowStart is required") Instant windowStart,
        @NotNull(message = "windowEnd is required") Instant windowEnd,
        @Min(value = 10, message = "slotDurationMinutes must be at least 10")
        @Max(value = 120, message = "slotDurationMinutes cannot exceed 120")
        int slotDurationMinutes
) {}