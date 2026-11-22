// RegisterRequestDto.java
package com.medslot.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "fullName is required") String fullName,
        @NotBlank(message = "email is required") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}