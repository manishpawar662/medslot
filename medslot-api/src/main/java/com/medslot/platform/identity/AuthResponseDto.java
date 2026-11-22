// AuthResponseDto.java
package com.medslot.platform.identity;

import com.medslot.platform.identity.security.AuthenticatedUser;
import java.util.UUID;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        UUID userId,
        String email,
        UserRole role
) {
    public static AuthResponseDto bearer(String token, AuthenticatedUser user) {
        return new AuthResponseDto(token, "Bearer", user.id(), user.email(), user.role());
    }
}