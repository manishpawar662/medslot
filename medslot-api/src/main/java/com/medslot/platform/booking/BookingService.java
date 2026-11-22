package com.medslot.platform.booking;

import com.medslot.platform.common.BusinessConflictException;
import com.medslot.platform.common.ResourceNotFoundException;
import com.medslot.platform.identity.User;
import com.medslot.platform.identity.UserRepository;
import com.medslot.platform.identity.UserRole;
import com.medslot.platform.scheduling.SlotStatus;
import com.medslot.platform.scheduling.TimeSlot;
import com.medslot.platform.scheduling.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppointmentResponseDto bookAppointment(BookAppointmentRequestDto request, UUID patientId) {
        TimeSlot slot = timeSlotRepository.findById(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found: " + request.slotId()));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new BusinessConflictException("Time slot is no longer available");
        }

        if (!slot.getStartTime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Cannot book a time slot in the past");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException("Only users with PATIENT role can book appointments");
        }

        slot.setStatus(SlotStatus.BOOKED);

        Appointment appointment = new Appointment();
        appointment.setSlot(slot);
        appointment.setPatient(patient);
        appointment.setClinic(slot.getClinic());
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setReason(request.reason());

        Appointment saved = appointmentRepository.save(appointment);
        return AppointmentResponseDto.from(saved);
    }

    @Transactional
    public AppointmentResponseDto cancelAppointment(UUID appointmentId, UUID patientId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("You can only cancel your own appointments");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessConflictException("Only CONFIRMED appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(Instant.now());

        // Release the slot back to AVAILABLE within the exact same ACID transaction
        appointment.getSlot().setStatus(SlotStatus.AVAILABLE);

        return AppointmentResponseDto.from(appointment);
    }
}