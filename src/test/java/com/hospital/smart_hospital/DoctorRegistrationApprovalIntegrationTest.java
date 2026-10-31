package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.DoctorRegistrationRequest;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.StaffAccountRequest;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
    @DisplayName("Verify Public Doctor Registration is Protected and Admin Flow Operates Securely")
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

        // 1. Unauthenticated public registration is blocked (HTTP 401 Unauthorized)
        mockMvc.perform(post("/api/auth/register-doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isUnauthorized());

        // 2. Admin authenticated registration succeeds and returns HTTP 201
        mockMvc.perform(post("/api/auth/register-doctor")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(testUsername)))
                .andExpect(jsonPath("$.data.approvalStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.enabled", is(false)));

        // 3. Pending doctor attempts to log in -> blocked with HTTP 401 & descriptive approval message
        LoginRequest loginAttempt = new LoginRequest(testUsername, "Sherlock#1887");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("pending administrator approval")));

        // 4. Duplicate checks: Registering again with same username fails with 409 Conflict
        mockMvc.perform(post("/api/auth/register-doctor")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // 5. Duplicate email check
        DoctorRegistrationRequest diffUserSameEmail = new DoctorRegistrationRequest(
                "Dr. Another Watson",
                "dr.watson2." + System.currentTimeMillis(),
                testEmail,
                "Neurology",
                "MBBS",
                "MRN-DIFF-" + System.currentTimeMillis(),
                "Pass@12345"
        );
        mockMvc.perform(post("/api/auth/register-doctor")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diffUserSameEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email")));

        // 6. Duplicate medical registration number check
        DoctorRegistrationRequest diffUserSameMedReg = new DoctorRegistrationRequest(
                "Dr. Third Watson",
                "dr.watson3." + System.currentTimeMillis(),
                "watson3." + System.currentTimeMillis() + "@test.org",
                "Cardiology",
                "MBBS",
                testMedReg,
                "Pass@12345"
        );
        mockMvc.perform(post("/api/auth/register-doctor")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(diffUserSameMedReg)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Medical Registration Number")));

        // 7. Non-admin cannot view pending doctors (HTTP 401/403)
        mockMvc.perform(get("/api/auth/doctors/pending"))
                .andExpect(status().isUnauthorized());

        User docInDb = userRepository.findByUsernameIgnoreCase(testUsername).orElseThrow();
        Long doctorId = docInDb.getId();

        // 8. Admin views pending doctor list
        mockMvc.perform(get("/api/auth/doctors/pending")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.notNullValue()));

        // 9. Admin approves the doctor account
        mockMvc.perform(patch("/api/auth/doctors/" + doctorId + "/approve")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.enabled", is(true)));

        // 10. Approved doctor logs in successfully -> HTTP 200
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
                "Password@123"
        );

        // Admin registers doctor
        mockMvc.perform(post("/api/auth/register-doctor")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        User rejectedDoc = userRepository.findByUsernameIgnoreCase(testUsername).orElseThrow();

        // Admin rejects doctor
        mockMvc.perform(patch("/api/auth/doctors/" + rejectedDoc.getId() + "/reject")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.approvalStatus", is("REJECTED")))
                .andExpect(jsonPath("$.data.enabled", is(false)));

        // Rejected doctor attempts to log in
        LoginRequest loginAttempt = new LoginRequest(testUsername, "Password@123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("rejected")));
    }

    @Test
    @DisplayName("Admin Staff Provisioning creates active Doctor with temporary password and full doctor role")
    void testAdminDirectStaffProvisioningForDoctor() throws Exception {
        String testUsername = "dr.provisioned." + System.currentTimeMillis();
        String testEmail = "provisioned." + System.currentTimeMillis() + "@hospital.org";
        String testMed = "MED-PROV-" + System.currentTimeMillis();

        StaffAccountRequest staffReq = new StaffAccountRequest(
                "Dr. Provisioned Staff",
                testUsername,
                testEmail,
                Role.ROLE_DOCTOR,
                "TempStaff@123",
                "Neurology",
                "MD, PhD",
                testMed
        );

        mockMvc.perform(post("/api/auth/staff")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(testUsername)))
                .andExpect(jsonPath("$.data.role", is("ROLE_DOCTOR")))
                .andExpect(jsonPath("$.data.enabled", is(true)))
                .andExpect(jsonPath("$.data.passwordChangeRequired", is(true)));

        // Doctor logs in immediately with temporary credentials
        LoginRequest loginAttempt = new LoginRequest(testUsername, "TempStaff@123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("ROLE_DOCTOR")))
                .andExpect(jsonPath("$.data.passwordChangeRequired", is(true)));
    }
}
