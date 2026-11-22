package com.medslot.platform.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponseDto.of(404, "Not Found", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler({
            BusinessConflictException.class,
            ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiErrorResponseDto> handleConflict(Exception ex, HttpServletRequest req) {
        String message = (ex instanceof ObjectOptimisticLockingFailureException)
                ? "This time slot was just modified or booked by another request. Please refresh and choose another slot."
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponseDto.of(409, "Conflict", message, req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponseDto> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponseDto.of(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        ApiErrorResponseDto body = new ApiErrorResponseDto(
                Instant.now(), 400, "Validation Failed", "Request validation failed", req.getRequestURI(), errors
        );
        return ResponseEntity.badRequest().body(body);
    }
}