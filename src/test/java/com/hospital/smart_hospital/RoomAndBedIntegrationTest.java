package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.BedController;
import com.hospital.smart_hospital.controller.RoomController;
import com.hospital.smart_hospital.dto.BedRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RoomAndBedIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private RoomController roomController;

    @Autowired
    private BedController bedController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController, bedController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        bedRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete workflow: Room and Bed creation, validation, retrieval, and status updates")
    void testRoomAndBedWorkflow() throws Exception {
        // 1. Create a new Room
        RoomRequest roomRequest = new RoomRequest("101-A", "ICU", "Critical Care");

        String roomResponseString = mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.roomNumber", is("101-A")))
                .andExpect(jsonPath("$.data.roomType", is("ICU")))
                .andExpect(jsonPath("$.data.wardDepartment", is("Critical Care")))
                .andReturn().getResponse().getContentAsString();

        // Extract generated roomId
        long roomId = objectMapper.readTree(roomResponseString).path("data").path("id").asLong();

        // 2. Validate duplicate room number prevention (Expect 409 Conflict)
        RoomRequest duplicateRoomRequest = new RoomRequest("101-A", "GENERAL", "Cardiology");
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRoomRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Room number '101-A' already exists.")));

        // 3. Create Bed 1 in Room 101-A
        BedRequest bed1Request = new BedRequest("BED-1", roomId, BedStatus.AVAILABLE);
        String bed1ResponseString = mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bed1Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bedNumber", is("BED-1")))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.data.roomId", is((int) roomId)))
                .andExpect(jsonPath("$.data.roomType", is("ICU")))
                .andReturn().getResponse().getContentAsString();

        long bed1Id = objectMapper.readTree(bed1ResponseString).path("data").path("id").asLong();

        // 4. Validate duplicate bed number in same room prevention (Expect 409 Conflict)
        BedRequest duplicateBedRequest = new BedRequest("BED-1", roomId, BedStatus.AVAILABLE);
        mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateBedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Bed number 'BED-1' already exists in Room 101-A.")));

        // 5. Create Bed 2 in Room 101-A
        BedRequest bed2Request = new BedRequest("BED-2", roomId, BedStatus.AVAILABLE);
        mockMvc.perform(post("/api/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bed2Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bedNumber", is("BED-2")));

        // 6. View Room and verify totalBeds & availableBeds counts
        mockMvc.perform(get("/api/rooms/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBeds", is(2)))
                .andExpect(jsonPath("$.data.availableBeds", is(2)));

        // 7. Update Bed 1 status to OCCUPIED
        mockMvc.perform(patch("/api/beds/" + bed1Id + "/status")
                        .param("status", "OCCUPIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("OCCUPIED")));

        // 8. Re-check Room bed counters: 2 total, 1 available
        mockMvc.perform(get("/api/rooms/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBeds", is(2)))
                .andExpect(jsonPath("$.data.availableBeds", is(1)));

        // 9. List beds by room ID
        mockMvc.perform(get("/api/rooms/" + roomId + "/beds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 10. Filter beds by status: OCCUPIED
        mockMvc.perform(get("/api/beds")
                        .param("status", "OCCUPIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-1")));

        // 11. Update room info
        RoomRequest updateRoom = new RoomRequest("101-A-RENAMED", "ICU-STEPDOWN", "Critical Care");
        mockMvc.perform(put("/api/rooms/" + roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRoom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomNumber", is("101-A-RENAMED")))
                .andExpect(jsonPath("$.data.roomType", is("ICU-STEPDOWN")));
    }
}
