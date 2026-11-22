package com.medslot.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.medslot.platform.AbstractPostgresContainerTest;
import com.medslot.platform.booking.AppointmentRepository;
import com.medslot.platform.booking.BookAppointmentRequestDto;
import com.medslot.platform.clinic.Clinic;
import com.medslot.platform.clinic.ClinicRepository;
import com.medslot.platform.clinic.Doctor;
import com.medslot.platform.clinic.DoctorRepository;
import com.medslot.platform.identity.security.AuthenticatedUser;
import com.medslot.platform.identity.security.JwtTokenProvider;
import com.medslot.platform.scheduling.CreateSlotRequestDto;
import com.medslot.platform.scheduling.TimeSlotRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper objectMapper;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private Doctor savedDoctor;
    private String doctorJwt;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Clinic clinic = new Clinic();
        clinic.setName("Apollo Care " + suffix);
        clinic.setCode("APC-" + suffix);
        clinic.setTimezone("Asia/Kolkata");
        Clinic savedClinic = clinicRepository.save(clinic);

        User doctorUser = new User();
        doctorUser.setEmail("dr.verma." + suffix + "@medslot.io");
        doctorUser.setPasswordHash(passwordEncoder.encode("DoctorPass@123"));
        doctorUser.setFullName("Dr. Vikram Verma");
        doctorUser.setRole(UserRole.DOCTOR);
        User savedDoctorUser = userRepository.save(doctorUser);

        Doctor doctor = new Doctor();
        doctor.setUser(savedDoctorUser);
        doctor.setClinic(savedClinic);
        doctor.setSpecialization("Dermatology");
        doctor.setConsultationFee(new BigDecimal("650.00"));
        savedDoctor = doctorRepository.save(doctor);

        doctorJwt = jwtTokenProvider.generateToken(AuthenticatedUser.from(savedDoctorUser));
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
    void shouldRejectUnauthenticatedBookingWith401() throws Exception {
        BookAppointmentRequestDto request = new BookAppointmentRequestDto(UUID.randomUUID(), "Checkup");

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldEnforceRbacAndAllowEndToEndJwtBookingFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String patientEmail = "rohan." + suffix + "@medslot.io";

        // 1. Register a new patient
        RegisterRequestDto registerReq = new RegisterRequestDto("Rohan Mehta", patientEmail, "PatientPass@123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 2. Login to obtain JWT
        LoginRequestDto loginReq = new LoginRequestDto(patientEmail, "PatientPass@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String patientJwt = loginJson.get("accessToken").asText();
        String expectedPatientId = loginJson.get("userId").asText();

        // 3. RBAC Check: Patient attempting to create a doctor slot must fail with 403 Forbidden
        CreateSlotRequestDto slotReq = new CreateSlotRequestDto(
                savedDoctor.getId(),
                Instant.now().plus(3, ChronoUnit.DAYS),
                Instant.now().plus(3, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES)
        );
        mockMvc.perform(post("/api/v1/slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotReq)))
                .andExpect(status().isForbidden());

        // 4. Doctor creates the slot -> 201 Created
        MvcResult slotResult = mockMvc.perform(post("/api/v1/slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctorJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID slotId = UUID.fromString(
                objectMapper.readTree(slotResult.getResponse().getContentAsString()).get("id").asText()
        );

        // 5. Patient books the slot -> patientId is extracted strictly from JWT principal
        BookAppointmentRequestDto bookReq = new BookAppointmentRequestDto(slotId, "Skin allergy consultation");
        MvcResult bookResult = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.patientId").value(expectedPatientId))
                .andReturn();

        assertThat(bookResult.getResponse().getStatus()).isEqualTo(201);
    }
}