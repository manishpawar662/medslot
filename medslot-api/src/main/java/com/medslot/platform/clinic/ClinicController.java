package com.medslot.platform.clinic;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    public ResponseEntity<ClinicResponseDto> createClinic(@Valid @RequestBody CreateClinicRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.createClinic(request));
    }

    @GetMapping
    public ResponseEntity<List<ClinicResponseDto>> listClinics() {
        return ResponseEntity.ok(clinicService.getAllClinics());
    }

    @PostMapping("/{clinicId}/doctors")
    public ResponseEntity<DoctorResponseDto> onboardDoctor(
            @PathVariable UUID clinicId,
            @Valid @RequestBody OnboardDoctorRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.onboardDoctor(clinicId, request));
    }

    @GetMapping("/{clinicId}/doctors")
    public ResponseEntity<List<DoctorResponseDto>> listDoctors(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(clinicService.getDoctorsByClinic(clinicId));
    }
}