package com.medslot.platform.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BookAppointmentRequestDto(
        @NotNull(message = "slotId is required") UUID slotId,
        @Size(max = 500, message = "reason cannot exceed 500 characters") String reason
) {}