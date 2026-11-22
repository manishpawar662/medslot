package com.medslot.platform.scheduling;

import com.medslot.platform.clinic.Doctor;
import com.medslot.platform.clinic.DoctorRepository;
import com.medslot.platform.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
}