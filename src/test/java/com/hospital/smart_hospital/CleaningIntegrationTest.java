package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.BedController;
import com.hospital.smart_hospital.controller.CleaningController;
import com.hospital.smart_hospital.controller.PatientController;
import com.hospital.smart_hospital.controller.RoomController;
import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.exception.GlobalExceptionHandler;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.BedCleaningLogRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CleaningIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private RoomController roomController;

    @Autowired
    private BedController bedController;

    @Autowired
    private PatientController patientController;

    @Autowired
    private CleaningController cleaningController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private BedCleaningLogRepository bedCleaningLogRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.hospital.smart_hospital.repository.ConsultationRepository consultationRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController, bedController, patientController, cleaningController)
                .setControllerAdvice(globalExceptionHandler)
                .build();

        consultationRepository.deleteAll();
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        bedCleaningLogRepository.deleteAll();
        bedRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End: Discharge transitions bed to DIRTY, assigns cleaner, sanitizes bed to AVAILABLE, and calculates stats")
    void testCleaningLifecycleEndToEnd() throws Exception {
        // 1. Create Room (ICU with urgent priority 1)
        RoomRequest roomRequest = new RoomRequest("ICU-101", "ICU", "ICU");
        String roomRes = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(roomRes).path("data").path("id").asLong();

        // 2. Create Bed in ICU
        BedRequest bedRequest = new BedRequest("BED-ICU-1", roomId, BedStatus.AVAILABLE);
        String bedRes = mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bedRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bedId = objectMapper.readTree(bedRes).path("data").path("id").asLong();

        // 3. Admit patient
        PatientAdmissionRequest admitReq = new PatientAdmissionRequest("PAT-CLEAN-01", "Gordon Freeman", 42, "MALE", "Trauma", bedId);
        String patientRes = mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long patientId = objectMapper.readTree(patientRes).path("data").path("id").asLong();

        // Bed is now OCCUPIED
        mockMvc.perform(get("/api/beds/" + bedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("OCCUPIED")));

        // 4. Discharge patient -> Bed transitions to DIRTY (Priority 1 for ICU)
        mockMvc.perform(post("/api/patients/" + patientId + "/discharge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DISCHARGED")));

        // Verify Bed is DIRTY and unavailable
        mockMvc.perform(get("/api/beds/" + bedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DIRTY")));

        // Verify bed appears in the Cleaning Queue with priority 1
        mockMvc.perform(get("/api/cleaning/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-ICU-1")))
                .andExpect(jsonPath("$.data[0].status", is("DIRTY")))
                .andExpect(jsonPath("$.data[0].priority", is(1)));

        // 5. Attempting to admit another patient to the DIRTY bed is REJECTED
        PatientAdmissionRequest admitFailReq = new PatientAdmissionRequest("PAT-CLEAN-02", "Eli Vance", 58, "MALE", "Observation", bedId);
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitFailReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is DIRTY and requires cleaning before admission")));

        // 6. Assign cleaner to the DIRTY bed
        mockMvc.perform(patch("/api/cleaning/" + bedId + "/assign")
                        .param("cleanerName", "Cleaner Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedTo", is("Cleaner Alice")))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));

        // 7. Complete cleaning with inspection notes
        mockMvc.perform(patch("/api/cleaning/" + bedId + "/complete")
                        .param("cleanerName", "Cleaner Alice")
                        .param("notes", "Deep sanitized with UV disinfection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));

        // 8. Verify bed is now AVAILABLE
        mockMvc.perform(get("/api/beds/" + bedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));

        // Cleaning queue is now empty
        mockMvc.perform(get("/api/cleaning/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 9. Now the second patient CAN be admitted successfully
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitFailReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("ADMITTED")))
                .andExpect(jsonPath("$.data.bedNumber", is("BED-ICU-1")));

        // 10. Check cleaning statistics
        mockMvc.perform(get("/api/cleaning/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedToday", is(1)))
                .andExpect(jsonPath("$.data.totalDirtyBeds", is(0)));
    }
}
