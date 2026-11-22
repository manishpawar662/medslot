package com.medslot.platform.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medslot.platform.identity.security.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/slots")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @PostMapping
    public ResponseEntity<TimeSlotResponseDto> createSlot(@Valid @RequestBody CreateSlotRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.createSlot(request));
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeSlotResponseDto>> getAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return ResponseEntity.ok(schedulingService.getAvailableSlots(doctorId, from, to));
    }
    
    @PostMapping("/bulk")
    public ResponseEntity<List<TimeSlotResponseDto>> generateBulkSlots(
            @Valid @RequestBody BulkCreateSlotsRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.generateBulkSlots(request));
    }
    
    @GetMapping("/my-schedule")
    public ResponseEntity<List<DoctorScheduleSlotDto>> getMySchedule(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.ok(schedulingService.getMySchedule(currentUser.id(), from, to));
    }
}