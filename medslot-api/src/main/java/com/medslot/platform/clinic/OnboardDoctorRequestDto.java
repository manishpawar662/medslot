// OnboardDoctorRequestDto.java
package com.medslot.platform.clinic;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OnboardDoctorRequestDto(
        @NotBlank(message = "fullName is required") String fullName,
        @NotBlank(message = "email is required") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank(message = "specialization is required") String specialization,
        @NotNull(message = "consultationFee is required") @DecimalMin(value = "0.0", inclusive = true) BigDecimal consultationFee
) {}