package com.medslot.platform.clinic;

import com.medslot.platform.AbstractPostgresContainerTest;
import com.medslot.platform.booking.AppointmentRepository;
import com.medslot.platform.identity.User;
import com.medslot.platform.identity.UserRepository;
import com.medslot.platform.identity.UserRole;
import com.medslot.platform.identity.security.AuthenticatedUser;
import com.medslot.platform.identity.security.JwtTokenProvider;
import com.medslot.platform.scheduling.BulkCreateSlotsRequestDto;
import com.medslot.platform.scheduling.TimeSlotRepository;
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
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClinicOnboardingAndBulkSlotIntegrationTest extends AbstractPostgresContainerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper objectMapper;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String adminJwt;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User admin = new User();
        admin.setFullName("Sys Admin");
        admin.setEmail("admin." + suffix + "@medslot.io");
        admin.setPasswordHash(passwordEncoder.encode("AdminPass@123"));
        admin.setRole(UserRole.ADMIN);
        User savedAdmin = userRepository.save(admin);

        adminJwt = jwtTokenProvider.generateToken(AuthenticatedUser.from(savedAdmin));
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
    void shouldOnboardClinicAndDoctorAndGenerateBulkSlotsIdempotently() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. ADMIN creates a clinic
        CreateClinicRequestDto clinicReq = new CreateClinicRequestDto(
                "Manipal Health " + suffix,
                "MNH-" + suffix,
                "Asia/Kolkata"
        );
        MvcResult clinicRes = mockMvc.perform(post("/api/v1/clinics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clinicReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                .andReturn();

        UUID clinicId = UUID.fromString(
                objectMapper.readTree(clinicRes.getResponse().getContentAsString()).get("id").asText()
        );

        // 2. ADMIN onboards a doctor under that clinic
        OnboardDoctorRequestDto doctorReq = new OnboardDoctorRequestDto(
                "Dr. Ananya Rao",
                "dr.rao." + suffix + "@medslot.io",
                "DoctorPass@123",
                "Cardiology",
                new BigDecimal("900.00")
        );
        MvcResult doctorRes = mockMvc.perform(post("/api/v1/clinics/" + clinicId + "/doctors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialization").value("Cardiology"))
                .andReturn();

        UUID doctorId = UUID.fromString(
                objectMapper.readTree(doctorRes.getResponse().getContentAsString()).get("doctorId").asText()
        );

        // 3. Bulk generate 4 x 30-minute slots across a 2-hour window
        Instant windowStart = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant windowEnd = windowStart.plus(2, ChronoUnit.HOURS);
        BulkCreateSlotsRequestDto bulkReq = new BulkCreateSlotsRequestDto(doctorId, windowStart, windowEnd, 30);

        mockMvc.perform(post("/api/v1/slots/bulk")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(4)));

        // 4. Re-running the exact same bulk window is idempotent -> returns 0 newly created slots
        mockMvc.perform(post("/api/v1/slots/bulk")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Public discovery confirms 1 doctor and 4 available slots
        mockMvc.perform(get("/api/v1/clinics/" + clinicId + "/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/slots/available")
                        .param("doctorId", doctorId.toString())
                        .param("from", windowStart.minus(5, ChronoUnit.MINUTES).toString())
                        .param("to", windowEnd.plus(5, ChronoUnit.MINUTES).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }
}