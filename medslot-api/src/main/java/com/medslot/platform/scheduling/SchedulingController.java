package com.medslot.platform.scheduling;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
}