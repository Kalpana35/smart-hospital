package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.AuthController;
import com.hospital.smart_hospital.controller.PatientController;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.PatientSelfRegistrationRequest;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.exception.GlobalExceptionHandler;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.PatientService;
import com.hospital.smart_hospital.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PatientSelfRegistrationAndOwnershipIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BedService bedService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long bed1Id;
    private Long bed2Id;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create rooms & beds for integration test
        var room = roomService.createRoom(new RoomRequest("REG-ROOM-1", "ICU", "Critical Care"));
        var b1 = bedService.createBed(new BedRequest("B-REG-1", room.getId(), BedStatus.AVAILABLE));
        var b2 = bedService.createBed(new BedRequest("B-REG-2", room.getId(), BedStatus.AVAILABLE));
        bed1Id = b1.getId();
        bed2Id = b2.getId();
    }

    @Test
    @DisplayName("Complete Patient Self-Registration and Record Ownership Flow")
    void testPatientRegistrationAndOwnershipEnforcement() throws Exception {
        // 1. Admit patient PAT-REG-01 ("Alice Wonder")
        PatientAdmissionRequest admitReq = new PatientAdmissionRequest(
                "PAT-REG-01", "Alice Wonder", 32, "FEMALE", "Observation", bed1Id
        );
        var admittedPatient = patientService.admitPatient(admitReq);
        long patientId1 = admittedPatient.getId();

        // 2. Admit patient PAT-REG-02 ("Bob Builder")
        PatientAdmissionRequest admitReq2 = new PatientAdmissionRequest(
                "PAT-REG-02", "Bob Builder", 45, "MALE", "Fracture", bed2Id
        );
        var admittedPatient2 = patientService.admitPatient(admitReq2);
        long patientId2 = admittedPatient2.getId();

        // 3. Attempt self-registration with non-existent MRN -> Reject 400
        PatientSelfRegistrationRequest badCodeReq = new PatientSelfRegistrationRequest(
                "fake.user", "Secret@123", "Nobody", "PAT-NONEXISTENT"
        );
        mockMvc.perform(post("/api/auth/register-patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCodeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("No hospital record found with Patient Code")));

        // 4. Attempt self-registration with wrong name -> Reject 400
        PatientSelfRegistrationRequest wrongNameReq = new PatientSelfRegistrationRequest(
                "alice.fake", "Secret@123", "Wrong Name", "PAT-REG-01"
        );
        mockMvc.perform(post("/api/auth/register-patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongNameReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Full name does not match hospital records")));

        // 5. Valid self-registration for Alice Wonder -> Success 201
        PatientSelfRegistrationRequest validReq = new PatientSelfRegistrationRequest(
                "alice.registered", "Secret@123", "Alice Wonder", "PAT-REG-01"
        );
        mockMvc.perform(post("/api/auth/register-patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("alice.registered")))
                .andExpect(jsonPath("$.data.role", is("ROLE_PATIENT")));

        // 6. Log in as alice.registered and verify session
        LoginRequest loginReq = new LoginRequest("alice.registered", "Secret@123");
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // 7. GET /api/patients/me returns Alice's record with Bed B-REG-1
        mockMvc.perform(get("/api/patients/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.patientCode", is("PAT-REG-01")))
                .andExpect(jsonPath("$.data.name", is("Alice Wonder")))
                .andExpect(jsonPath("$.data.bedNumber", is("B-REG-1")));

        // 8. GET /api/patients/{patientId1} (own record) -> Allowed 200
        mockMvc.perform(get("/api/patients/" + patientId1).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientCode", is("PAT-REG-01")));

        // 9. GET /api/patients/{patientId2} (Bob's record) -> Forbidden 403 (Ownership protection)
        mockMvc.perform(get("/api/patients/" + patientId2).session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access Denied: You cannot view records of other patients")));
    }
}
