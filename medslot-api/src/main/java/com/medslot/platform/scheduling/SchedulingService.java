package com.medslot.platform.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medslot.platform.clinic.Doctor;
import com.medslot.platform.clinic.DoctorRepository;
import com.medslot.platform.common.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public TimeSlotResponseDto createSlot(CreateSlotRequestDto request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime must be strictly after startTime");
        }

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.doctorId()));

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setClinic(doctor.getClinic());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setStatus(SlotStatus.AVAILABLE);

        TimeSlot saved = timeSlotRepository.save(slot);
        return TimeSlotResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponseDto> getAvailableSlots(UUID doctorId, Instant from, Instant to) {
        return timeSlotRepository.findAvailableSlots(doctorId, from, to)
                .stream()
                .map(TimeSlotResponseDto::from)
                .toList();
    }
    
    @Transactional
    public List<TimeSlotResponseDto> generateBulkSlots(BulkCreateSlotsRequestDto request) {
        if (!request.windowEnd().isAfter(request.windowStart())) {
            throw new IllegalArgumentException("windowEnd must be strictly after windowStart");
        }
        if (!request.windowStart().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Cannot generate slots in the past");
        }

        Duration maxShift = Duration.ofHours(12);
        if (Duration.between(request.windowStart(), request.windowEnd()).compareTo(maxShift) > 0) {
            throw new IllegalArgumentException("Bulk generation window cannot exceed 12 hours per request");
        }

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.doctorId()));

        Set<Instant> existingStarts = timeSlotRepository.findExistingStartTimes(
                doctor.getId(), request.windowStart(), request.windowEnd()
        );

        Duration step = Duration.ofMinutes(request.slotDurationMinutes());
        List<TimeSlot> toCreate = new ArrayList<>();
        Instant cursor = request.windowStart();

        while (!cursor.plus(step).isAfter(request.windowEnd())) {
            if (!existingStarts.contains(cursor)) {
                TimeSlot slot = new TimeSlot();
                slot.setDoctor(doctor);
                slot.setClinic(doctor.getClinic());
                slot.setStartTime(cursor);
                slot.setEndTime(cursor.plus(step));
                slot.setStatus(SlotStatus.AVAILABLE);
                toCreate.add(slot);
            }
            cursor = cursor.plus(step);
        }

        return timeSlotRepository.saveAll(toCreate).stream()
                .map(TimeSlotResponseDto::from)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<DoctorScheduleSlotDto> getMySchedule(UUID doctorUserId, Instant from, Instant to) {
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("'to' timestamp must be strictly after 'from'");
        }
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + doctorUserId));

        return timeSlotRepository.findDoctorSchedule(doctor.getId(), from, to).stream()
                .map(DoctorScheduleSlotDto::from)
                .toList();
    }
}