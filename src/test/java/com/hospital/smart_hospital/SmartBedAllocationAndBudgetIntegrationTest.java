package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.BedAllocationController;
import com.hospital.smart_hospital.controller.BedController;
import com.hospital.smart_hospital.controller.PatientController;
import com.hospital.smart_hospital.controller.RoomController;
import com.hospital.smart_hospital.dto.BedRecommendationRequest;
import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.exception.GlobalExceptionHandler;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Department;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.BedCleaningLogRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.DepartmentRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SmartBedAllocationAndBudgetIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private BedAllocationController bedAllocationController;

    @Autowired
    private PatientController patientController;

    @Autowired
    private RoomController roomController;

    @Autowired
    private BedController bedController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BedCleaningLogRepository bedCleaningLogRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private com.hospital.smart_hospital.repository.ConsultationRepository consultationRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Room icuRoom;
    private Room generalRoom;
    private Room isolationRoom;
    private Bed icuBed1;
    private Bed icuBed2;
    private Bed generalBed1;
    private Bed isolationBed1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bedAllocationController, patientController, roomController, bedController)
                .setControllerAdvice(globalExceptionHandler)
                .build();

        consultationRepository.deleteAll();
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        bedCleaningLogRepository.deleteAll();
        bedRepository.deleteAll();
        roomRepository.deleteAll();

        // Ensure departments exist or create them
        Department icuDept = departmentRepository.findByCode("ICU")
                .orElseGet(() -> departmentRepository.save(new Department("Intensive Care Unit", "ICU", "Critical care", "Dr. Vance")));
        Department genDept = departmentRepository.findByCode("GEN")
                .orElseGet(() -> departmentRepository.save(new Department("General Ward", "GEN", "General medical-surgical", "Dr. Breen")));
        Department isoDept = departmentRepository.findByCode("ISO")
                .orElseGet(() -> departmentRepository.save(new Department("Isolation Unit", "ISO", "Infectious containment", "Dr. Mossman")));

        // Create rooms
        icuRoom = new Room("ICU-01", "ICU", "ICU", icuDept, 1500.0, true, false);
        icuRoom = roomRepository.save(icuRoom);

        generalRoom = new Room("GEN-01", "GENERAL", "General Ward", genDept, 400.0, false, false);
        generalRoom = roomRepository.save(generalRoom);

        isolationRoom = new Room("ISO-01", "ISOLATION", "Isolation Unit", isoDept, 1200.0, false, true);
        isolationRoom = roomRepository.save(isolationRoom);

        // Create beds
        icuBed1 = new Bed("BED-ICU-1", icuRoom, BedStatus.AVAILABLE, 1500.0, "VENTILATOR,VITAL_MONITOR");
        icuBed1 = bedRepository.save(icuBed1);

        icuBed2 = new Bed("BED-ICU-2", icuRoom, BedStatus.OCCUPIED, 1500.0, "VENTILATOR,VITAL_MONITOR");
        icuBed2 = bedRepository.save(icuBed2);

        generalBed1 = new Bed("BED-GEN-1", generalRoom, BedStatus.AVAILABLE, 400.0, "STANDARD_MONITOR");
        generalBed1 = bedRepository.save(generalBed1);

        isolationBed1 = new Bed("BED-ISO-1", isolationRoom, BedStatus.AVAILABLE, 1200.0, "NEGATIVE_PRESSURE,HEPA_FILTER");
        isolationBed1 = bedRepository.save(isolationBed1);
    }

    @Test
    @DisplayName("Smart Recommendation: Clinical matching requires ICU monitoring and excludes general beds")
    void testClinicalMatching_IcuMonitoring() throws Exception {
        BedRecommendationRequest request = new BedRecommendationRequest(
                "ICU", "ICU", "CRITICAL", true, false, 2000.0, false
        );

        mockMvc.perform(post("/api/allocations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-ICU-1")))
                .andExpect(jsonPath("$.data[0].hasIcuMonitoring", is(true)))
                .andExpect(jsonPath("$.data[0].matchScore", greaterThanOrEqualTo(100)));
    }

    @Test
    @DisplayName("Smart Recommendation: Isolation requirement strictly matches isolation negative-pressure rooms")
    void testClinicalMatching_IsolationSuitability() throws Exception {
        BedRecommendationRequest request = new BedRecommendationRequest(
                "ISOLATION", "Isolation Unit", "MODERATE", false, true, 1500.0, false
        );

        mockMvc.perform(post("/api/allocations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-ISO-1")))
                .andExpect(jsonPath("$.data[0].hasIsolation", is(true)));
    }

    @Test
    @DisplayName("Budget-Aware Recommendation: Evaluates preferred daily budget and marks withinBudget")
    void testBudgetAwareRecommendations() throws Exception {
        // Preferred budget $500/day for General Ward bed ($400/day)
        BedRecommendationRequest request = new BedRecommendationRequest(
                "GENERAL", "General Ward", "LOW", false, false, 500.0, false
        );

        mockMvc.perform(post("/api/allocations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-GEN-1")))
                .andExpect(jsonPath("$.data[0].withinBudget", is(true)))
                .andExpect(jsonPath("$.data[0].dailyRate", is(400.0)));

        // Preferred budget $200/day for General Ward bed ($400/day) -> exceeds budget
        BedRecommendationRequest overBudgetReq = new BedRecommendationRequest(
                "GENERAL", "General Ward", "LOW", false, false, 200.0, false
        );

        mockMvc.perform(post("/api/allocations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overBudgetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-GEN-1")))
                .andExpect(jsonPath("$.data[0].withinBudget", is(false)))
                .andExpect(jsonPath("$.data[0].matchReason", containsString("exceeds preferred budget")));
    }

    @Test
    @DisplayName("Bed Protection: Strictly excludes OCCUPIED, MAINTENANCE, and DIRTY beds from recommendations")
    void testExcludesUnavailableBeds() throws Exception {
        // Mark all hospital beds as unavailable (DIRTY, MAINTENANCE, OCCUPIED)
        generalBed1.setStatus(BedStatus.DIRTY);
        bedRepository.save(generalBed1);

        icuBed1.setStatus(BedStatus.MAINTENANCE);
        bedRepository.save(icuBed1);

        isolationBed1.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(isolationBed1);

        BedRecommendationRequest request = new BedRecommendationRequest(
                "GENERAL", "General Ward", "LOW", false, false, 500.0, false
        );

        // When no beds in any ward are AVAILABLE, recommendations must be empty
        mockMvc.perform(post("/api/allocations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Simultaneous Allocation Protection: Cannot admit multiple patients to the same bed")
    void testPreventDoubleAllocation() throws Exception {
        PatientAdmissionRequest req1 = new PatientAdmissionRequest(
                "PAT-ALPHA", "Alpha User", 30, "MALE", "Fever", icuBed1.getId()
        );
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Attempting to admit another patient to the same bed must be rejected
        PatientAdmissionRequest req2 = new PatientAdmissionRequest(
                "PAT-BETA", "Beta User", 32, "FEMALE", "Cough", icuBed1.getId()
        );
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already OCCUPIED")));
    }

    @Test
    @DisplayName("Predictive Discharge Invariant: Upcoming discharge surfaces pending availability without premature allocation")
    void testPredictiveDischarge_NoPrematureAvailability() throws Exception {
        // Admit patient with expected discharge within 24 hours
        LocalDateTime expectedDischarge = LocalDateTime.now().plusHours(12);
        PatientAdmissionRequest admitReq = new PatientAdmissionRequest(
                "PAT-PREDICT", "Predictive Patient", 65, "FEMALE", "Post-Op Cardiac",
                icuBed1.getId(), expectedDischarge
        );
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admitReq)))
                .andExpect(status().isCreated());

        // Query predictive discharges endpoint
        mockMvc.perform(get("/api/allocations/predictive-discharges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].patientCode", is("PAT-PREDICT")))
                .andExpect(jsonPath("$.data[0].bedNumber", is("BED-ICU-1")));

        // Invariant check: The bed is STILL OCCUPIED and CANNOT be recommended or allocated!
        BedRecommendationRequest recReq = new BedRecommendationRequest(
                "ICU", "ICU", "CRITICAL", true, false, 2000.0, false
        );
        mockMvc.perform(post("/api/allocations/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0))); // icuBed1 is NOT available yet

        PatientAdmissionRequest prematureAdmit = new PatientAdmissionRequest(
                "PAT-FAIL", "Too Early", 40, "MALE", "Observation", icuBed1.getId()
        );
        mockMvc.perform(post("/api/patients/admit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prematureAdmit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already OCCUPIED")));
    }
}
