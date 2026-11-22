// ClinicResponseDto.java
package com.medslot.platform.clinic;

import java.util.UUID;

public record ClinicResponseDto(
        UUID id,
        String name,
        String code,
        String timezone
) {
    public static ClinicResponseDto from(Clinic clinic) {
        return new ClinicResponseDto(
                clinic.getId(),
                clinic.getName(),
                clinic.getCode(),
                clinic.getTimezone()
        );
    }
}