package com.medslot.platform.booking;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        UUID slotId,
        UUID patientId,
        String doctorName,
        String clinicName,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        String reason,
        Instant createdAt
) {
    public static AppointmentResponseDto from(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getSlot().getId(),
                appointment.getPatient().getId(),
                appointment.getSlot().getDoctor().getUser().getFullName(),
                appointment.getClinic().getName(),
                appointment.getSlot().getStartTime(),
                appointment.getSlot().getEndTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt()
        );
    }
}