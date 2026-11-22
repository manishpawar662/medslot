package com.medslot.platform.booking;

import com.medslot.platform.AbstractPostgresContainerTest;
import com.medslot.platform.clinic.Clinic;
import com.medslot.platform.clinic.ClinicRepository;
import com.medslot.platform.clinic.Doctor;
import com.medslot.platform.clinic.DoctorRepository;
import com.medslot.platform.common.BusinessConflictException;
import com.medslot.platform.identity.User;
import com.medslot.platform.identity.UserRepository;
import com.medslot.platform.identity.UserRole;
import com.medslot.platform.scheduling.SlotStatus;
import com.medslot.platform.scheduling.TimeSlot;
import com.medslot.platform.scheduling.TimeSlotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookingConcurrencyIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired private BookingService bookingService;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    private Clinic savedClinic;
    private User savedDoctorUser;
    private Doctor savedDoctor;
    private TimeSlot savedSlot;
    private List<User> patients;

    private static final int CONCURRENT_PATIENTS = 10;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Clinic clinic = new Clinic();
        clinic.setName("City Care Clinic " + suffix);
        clinic.setCode("CCC-" + suffix);
        clinic.setTimezone("Asia/Kolkata");
        savedClinic = clinicRepository.save(clinic);

        User docUser = new User();
        docUser.setEmail("dr.sharma." + suffix + "@medslot.io");
        docUser.setPasswordHash("hashed_secret");
        docUser.setFullName("Dr. Aarav Sharma");
        docUser.setRole(UserRole.DOCTOR);
        savedDoctorUser = userRepository.save(docUser);

        Doctor doctor = new Doctor();
        doctor.setUser(savedDoctorUser);
        doctor.setClinic(savedClinic);
        doctor.setSpecialization("Cardiology");
        doctor.setConsultationFee(new BigDecimal("800.00"));
        savedDoctor = doctorRepository.save(doctor);

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(savedDoctor);
        slot.setClinic(savedClinic);
        slot.setStartTime(Instant.now().plus(2, ChronoUnit.DAYS));
        slot.setEndTime(Instant.now().plus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));
        slot.setStatus(SlotStatus.AVAILABLE);
        savedSlot = timeSlotRepository.save(slot);

        patients = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_PATIENTS; i++) {
            User patient = new User();
            patient.setEmail("patient." + i + "." + suffix + "@medslot.io");
            patient.setPasswordHash("hashed_secret");
            patient.setFullName("Patient " + i);
            patient.setRole(UserRole.PATIENT);
            patients.add(userRepository.save(patient));
        }
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
        timeSlotRepository.deleteAll();
        doctorRepository.deleteAll();
        userRepository.deleteAll();
        clinicRepository.deleteAll();
    }

    @Test
    void shouldAllowOnlyOneBookingWhenTenPatientsBookSameSlotSimultaneously() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_PATIENTS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_PATIENTS);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (User patient : patients) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // Wait until all 10 threads are poised at the starting line
                    startLatch.await();

                    bookingService.bookAppointment(
                            new BookAppointmentRequestDto(savedSlot.getId(), "Routine checkup"),
                            patient.getId()
                    );
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException
                         | BusinessConflictException
                         | DataIntegrityViolationException expectedConflict) {
                    conflictCount.incrementAndGet();
                } catch (Throwable unexpected) {
                    unexpectedErrors.add(unexpected);
                }
            });
        }

        // Wait for all threads to initialize, then fire them simultaneously
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Assertions: Exactly 1 winner, 9 clean conflict rejections, 0 unexpected errors
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(CONCURRENT_PATIENTS - 1);

        // Verify database state directly
        TimeSlot updatedSlot = timeSlotRepository.findById(savedSlot.getId()).orElseThrow();
        assertThat(updatedSlot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(updatedSlot.getVersion()).isEqualTo(1L);
        assertThat(appointmentRepository.count()).isEqualTo(1L);
    }
    
    @Test
    void shouldAllowRebookingSlotAfterCancellationWhilePreservingAuditHistory() {
        User patientA = patients.get(0);
        User patientB = patients.get(1);

        AppointmentResponseDto firstBooking = bookingService.bookAppointment(
                new BookAppointmentRequestDto(savedSlot.getId(), "Initial consultation"),
                patientA.getId()
        );
        assertThat(firstBooking.status()).isEqualTo(AppointmentStatus.CONFIRMED);

        AppointmentResponseDto cancelled = bookingService.cancelAppointment(firstBooking.id(), patientA.getId());
        assertThat(cancelled.status()).isEqualTo(AppointmentStatus.CANCELLED);

        AppointmentResponseDto secondBooking = bookingService.bookAppointment(
                new BookAppointmentRequestDto(savedSlot.getId(), "Follow-up visit"),
                patientB.getId()
        );
        assertThat(secondBooking.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointmentRepository.count()).isEqualTo(2L);
    }
}