package com.medslot.platform.booking;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medslot.platform.identity.security.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> book(
            @Valid @RequestBody BookAppointmentRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.bookAppointment(request, currentUser.id()));
    }

    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponseDto> cancel(
            @PathVariable UUID appointmentId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(bookingService.cancelAppointment(appointmentId, currentUser.id()));
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<PatientAppointmentViewDto>> getMyAppointments(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(bookingService.getMyAppointments(currentUser.id()));
    }
}