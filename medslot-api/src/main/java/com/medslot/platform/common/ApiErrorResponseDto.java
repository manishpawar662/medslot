// com/medslot/platform/common/ApiErrorResponse.java
package com.medslot.platform.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponseDto of(int status, String error, String message, String path) {
        return new ApiErrorResponseDto(Instant.now(), status, error, message, path, Map.of());
    }
}