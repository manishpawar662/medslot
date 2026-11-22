// DoctorResponseDto.java
package com.medslot.platform.clinic;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorResponseDto(
        UUID doctorId,
        UUID userId,
        UUID clinicId,
        String fullName,
        String email,
        String specialization,
        BigDecimal consultationFee
) {
    public static DoctorResponseDto from(Doctor doctor) {
        return new DoctorResponseDto(
                doctor.getId(),
                doctor.getUser().getId(),
                doctor.getClinic().getId(),
                doctor.getUser().getFullName(),
                doctor.getUser().getEmail(),
                doctor.getSpecialization(),
                doctor.getConsultationFee()
        );
    }
}