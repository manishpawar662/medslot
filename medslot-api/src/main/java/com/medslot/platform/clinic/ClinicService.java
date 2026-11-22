package com.medslot.platform.clinic;

import com.medslot.platform.common.BusinessConflictException;
import com.medslot.platform.common.ResourceNotFoundException;
import com.medslot.platform.identity.User;
import com.medslot.platform.identity.UserRepository;
import com.medslot.platform.identity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ClinicResponseDto createClinic(CreateClinicRequestDto request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (clinicRepository.existsByCode(normalizedCode)) {
            throw new BusinessConflictException("Clinic code already exists: " + normalizedCode);
        }

        ZoneId validZone;
        try {
            validZone = ZoneId.of(request.timezone().trim());
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid IANA timezone: " + request.timezone());
        }

        Clinic clinic = new Clinic();
        clinic.setName(request.name().trim());
        clinic.setCode(normalizedCode);
        clinic.setTimezone(validZone.getId());

        return ClinicResponseDto.from(clinicRepository.save(clinic));
    }

    @Transactional(readOnly = true)
    public List<ClinicResponseDto> getAllClinics() {
        return clinicRepository.findAll().stream()
                .map(ClinicResponseDto::from)
                .toList();
    }

    @Transactional
    public DoctorResponseDto onboardDoctor(UUID clinicId, OnboardDoctorRequestDto request) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessConflictException("Email is already registered: " + normalizedEmail);
        }

        User doctorUser = new User();
        doctorUser.setFullName(request.fullName().trim());
        doctorUser.setEmail(normalizedEmail);
        doctorUser.setPasswordHash(passwordEncoder.encode(request.password()));
        doctorUser.setRole(UserRole.DOCTOR);
        User savedUser = userRepository.save(doctorUser);

        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setClinic(clinic);
        doctor.setSpecialization(request.specialization().trim());
        doctor.setConsultationFee(request.consultationFee());

        return DoctorResponseDto.from(doctorRepository.save(doctor));
    }

    @Transactional(readOnly = true)
    public List<DoctorResponseDto> getDoctorsByClinic(UUID clinicId) {
        if (!clinicRepository.existsById(clinicId)) {
            throw new ResourceNotFoundException("Clinic not found: " + clinicId);
        }
        return doctorRepository.findAllByClinicIdWithDetails(clinicId).stream()
                .map(DoctorResponseDto::from)
                .toList();
    }
}