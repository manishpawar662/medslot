// CreateClinicRequestDto.java
package com.medslot.platform.clinic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClinicRequestDto(
        @NotBlank(message = "name is required") @Size(max = 150) String name,
        @NotBlank(message = "code is required") @Size(max = 50) String code,
        @NotBlank(message = "timezone is required") String timezone
) {}