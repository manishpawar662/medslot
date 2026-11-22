// LoginRequestDto.java
package com.medslot.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "email is required") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "password is required") String password
) {}