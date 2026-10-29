package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.BedController;
import com.hospital.smart_hospital.controller.PatientController;
import com.hospital.smart_hospital.controller.RoomController;
import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.exception.GlobalExceptionHandler;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.repository.AppointmentRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PatientAdmissionDischargeIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private RoomController roomController;

    @Autowired
    private BedController bedController;

    @Autowired
    private PatientController patientController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController, bedController, patientController)
                .setControllerAdvice(globalExceptionHandler)
                .build();

        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        bedRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End: Admission, dynamic room capacity, occupied bed rejection, discharge, and repeated discharge")
    void testPatientAdmissionAndDischargeFlow() throws Exception {
        // 1. Create Room
        RoomRequest roomRequest = new RoomRequest("301-ICU", "ICU", "Cardiology");
        String roomResStr = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long roomId = objectMapper.readTree(roomResStr).path("data").path("id").asLong();

        // 2. Create Bed 1 (AVAILABLE) and Bed 2 (MAINTENANCE)
        BedRequest bed1Request = new BedRequest("BED-1", roomId, BedStatus.AVAILABLE);
        String bed1ResStr = mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bed1Request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bed1Id = objectMapper.readTree(bed1ResStr).path("data").path("id").asLong();

        BedRequest bed2Request = new BedRequest("BED-2", roomId, BedStatus.MAINTENANCE);
        String bed2ResStr = mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bed2Request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bed2Id = objectMapper.readTree(bed2ResStr).path("data").path("id").asLong();

        // Verify initial room capacity: 2 total, 1 available
        mockMvc.perform(get("/api/rooms/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBeds", is(2)))
                .andExpect(jsonPath("$.data.availableBeds", is(1)));

        // 3. Reject admission to MAINTENANCE bed
        PatientAdmissionRequest maintBedPatient = new PatientAdmissionRequest(
                "PAT-MAINT", "Mark Stone", 45, "MALE", "Observation", bed2Id
        );
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintBedPatient)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("under MAINTENANCE")));

        // 4. Successful Admission to Bed 1 (AVAILABLE)
        PatientAdmissionRequest admitRequest = new PatientAdmissionRequest(
                "PAT-101", "John Doe", 52, "MALE", "Hypertension", bed1Id
        );
        String admitResStr = mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.patientCode", is("PAT-101")))
                .andExpect(jsonPath("$.data.status", is("ADMITTED")))
                .andExpect(jsonPath("$.data.bedId", is((int) bed1Id)))
                .andExpect(jsonPath("$.data.bedNumber", is("BED-1")))
                .andExpect(jsonPath("$.data.roomType", is("ICU")))
                .andExpect(jsonPath("$.data.admissionDate", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        long patientId = objectMapper.readTree(admitResStr).path("data").path("id").asLong();

        // 5. Verify Bed 1 is now OCCUPIED and room availableBeds dynamically drops to 0
        mockMvc.perform(get("/api/beds/" + bed1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("OCCUPIED")));

        mockMvc.perform(get("/api/rooms/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBeds", is(2)))
                .andExpect(jsonPath("$.data.availableBeds", is(0)));

        // 6. Reject admission of another patient to the already OCCUPIED Bed 1
        PatientAdmissionRequest occupiedBedPatient = new PatientAdmissionRequest(
                "PAT-102", "Jane Roe", 30, "FEMALE", "Migraine", bed1Id
        );
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(occupiedBedPatient)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already OCCUPIED")));

        // 6b. Prevent changing OCCUPIED bed to MAINTENANCE or AVAILABLE while patient is admitted
        mockMvc.perform(patch("/api/beds/" + bed1Id + "/status")
                        .param("status", "MAINTENANCE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is currently admitted to this bed. Discharge the patient first.")));

        mockMvc.perform(patch("/api/beds/" + bed1Id + "/status")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is currently admitted to this bed. Discharge the patient first.")));

        // 7. Successful Discharge of Patient
        mockMvc.perform(post("/api/patients/" + patientId + "/discharge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DISCHARGED")))
                .andExpect(jsonPath("$.data.dischargeDate", notNullValue()));

        // 8. Verify Bed 1 is now AVAILABLE and room availableBeds dynamically increments back to 1
        mockMvc.perform(get("/api/beds/" + bed1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));

        mockMvc.perform(get("/api/rooms/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBeds", is(2)))
                .andExpect(jsonPath("$.data.availableBeds", is(1)));

        // 9. Reject Repeated Discharge of the same patient
        mockMvc.perform(post("/api/patients/" + patientId + "/discharge"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is already discharged.")));
    }
}

