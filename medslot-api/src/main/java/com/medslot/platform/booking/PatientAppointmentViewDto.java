package com.medslot.platform.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PatientAppointmentViewDto(
        UUID appointmentId,
        AppointmentStatus status,
        String reason,
        UUID slotId,
        Instant startTime,
        Instant endTime,
        String doctorName,
        String specialization,
        BigDecimal consultationFee,
        String clinicName
) {
    public static PatientAppointmentViewDto from(Appointment appointment) {
        var slot = appointment.getSlot();
        var doctor = slot.getDoctor();
        return new PatientAppointmentViewDto(
                appointment.getId(),
                appointment.getStatus(),
                appointment.getReason(),
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                doctor.getUser().getFullName(),
                doctor.getSpecialization(),
                doctor.getConsultationFee(),
                appointment.getClinic().getName()
        );
    }
}