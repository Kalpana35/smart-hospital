package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.DoctorRegistrationRequest;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.model.DoctorApprovalStatus;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DoctorRegistrationApprovalIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Complete Doctor Sign-Up, Pending Restriction, Admin Approval & Login Flow")
    void testDoctorRegistrationApprovalAndLoginLifecycle() throws Exception {
        String testUsername = "dr.watson." + System.currentTimeMillis();
        String testEmail = "watson." + System.currentTimeMillis() + "@bakerstreet.org";
        String testMedReg = "MRN-WATSON-" + System.currentTimeMillis();

        DoctorRegistrationRequest regReq = new DoctorRegistrationRequest(
                "Dr. John Watson",
                testUsername,
                testEmail,
                "General Medicine",
                "MD, FACP",
                testMedReg,
                "Sherlock#1887"
        );

        // 1. Unauthenticated public registration succeeds and returns HTTP 201
        mockMvc.perform(post("/api/auth/register-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(testUsername)))
                .andExpect(jsonPath("$.data.approvalStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.enabled", is(false)));

        // 2. Pending doctor attempts to log in -> blocked with HTTP 401 & descriptive approval message
        LoginRequest loginAttempt = new LoginRequest(testUsername, "Sherlock#1887");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("pending administrator approval")));

        // 3. Duplicate checks: Registering again with same username fails with 409 Conflict
        mockMvc.perform(post("/api/auth/register-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // 4. Duplicate email check
        DoctorRegistrationRequest diffUserSameEmail = new DoctorRegistrationRequest(
                "Dr. Another Watson",
                "dr.watson2." + System.currentTimeMillis(),
                testEmail,
                "Neurology",
                "MBBS",
                "MRN-DIFF-" + System.currentTimeMillis(),
                "Pass12345"
        );
        mockMvc.perform(post("/api/auth/register-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diffUserSameEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email")));

        // 5. Duplicate medical registration number check
        DoctorRegistrationRequest diffUserSameMedReg = new DoctorRegistrationRequest(
                "Dr. Third Watson",
                "dr.watson3." + System.currentTimeMillis(),
                "watson3." + System.currentTimeMillis() + "@test.org",
                "Cardiology",
                "MBBS",
                testMedReg,
                "Pass12345"
        );
        mockMvc.perform(post("/api/auth/register-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diffUserSameMedReg)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Medical Registration Number")));

        // 6. Non-admin cannot view pending doctors (HTTP 403)
        mockMvc.perform(get("/api/auth/doctors/pending"))
                .andExpect(status().isUnauthorized());

        User docInDb = userRepository.findByUsernameIgnoreCase(testUsername).orElseThrow();
        Long doctorId = docInDb.getId();

        // 7. Admin views pending doctor list
        // Simulate ADMIN authenticated session
        mockMvc.perform(get("/api/auth/doctors/pending")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.notNullValue()));

        // 8. Admin approves the doctor account
        mockMvc.perform(patch("/api/auth/doctors/" + doctorId + "/approve")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.enabled", is(true)));

        // 9. Approved doctor logs in successfully -> HTTP 200
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(testUsername)))
                .andExpect(jsonPath("$.data.role", is("ROLE_DOCTOR")))
                .andExpect(jsonPath("$.data.enabled", is(true)));
    }

    @Test
    @DisplayName("Doctor Rejection Flow Blocks Login with Rejected Notice")
    void testDoctorRejectionLifecycle() throws Exception {
        String testUsername = "dr.reject." + System.currentTimeMillis();
        String testEmail = "reject." + System.currentTimeMillis() + "@hospital.org";
        String testMedReg = "MRN-REJ-" + System.currentTimeMillis();

        DoctorRegistrationRequest regReq = new DoctorRegistrationRequest(
                "Dr. Suspicious Applicant",
                testUsername,
                testEmail,
                "Surgery",
                "Unknown",
                testMedReg,
                "Password123"
        );

        mockMvc.perform(post("/api/auth/register-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        User rejectedDoc = userRepository.findByUsernameIgnoreCase(testUsername).orElseThrow();

        // Admin rejects doctor
        mockMvc.perform(patch("/api/auth/doctors/" + rejectedDoc.getId() + "/reject")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.approvalStatus", is("REJECTED")))
                .andExpect(jsonPath("$.data.enabled", is(false)));

        // Rejected doctor attempts to log in
        LoginRequest loginAttempt = new LoginRequest(testUsername, "Password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("rejected")));
    }
}
