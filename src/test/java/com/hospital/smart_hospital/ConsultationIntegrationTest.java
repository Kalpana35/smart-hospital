package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.ConsultationRequest;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.ConsultationStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.ConsultationRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.RoomService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ConsultationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BedService bedService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Patient patientA;
    private Patient patientB;
    private User doctorUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        consultationRepository.deleteAll();

        if (!userRepository.existsByUsernameIgnoreCase("test.doctor")) {
            doctorUser = new User("test.doctor", passwordEncoder.encode("secret123"), "Dr. Test Doctor", Role.ROLE_DOCTOR);
            doctorUser = userRepository.save(doctorUser);
        } else {
            doctorUser = userRepository.findByUsernameIgnoreCase("test.doctor").orElseThrow();
        }

        User patientUserA = userRepository.findByUsernameIgnoreCase("patient.a")
                .orElseGet(() -> userRepository.save(new User("patient.a", passwordEncoder.encode("secret123"), "Patient A", Role.ROLE_PATIENT)));

        User patientUserB = userRepository.findByUsernameIgnoreCase("patient.b")
                .orElseGet(() -> userRepository.save(new User("patient.b", passwordEncoder.encode("secret123"), "Patient B", Role.ROLE_PATIENT)));

        Room room = roomRepository.findAll().stream().findFirst().orElseGet(() -> {
            var r = roomService.createRoom(new RoomRequest("CONS-RM-99", "GENERAL", "Ward 1"));
            return roomRepository.findById(r.getId()).orElseThrow();
        });

        Bed bedA = bedRepository.findAll().stream().findFirst().orElseGet(() -> {
            var b = bedService.createBed(new BedRequest("CONS-BED-99A", room.getId(), BedStatus.OCCUPIED));
            return bedRepository.findById(b.getId()).orElseThrow();
        });

        Bed bedB = bedRepository.findAll().stream().skip(1).findFirst().orElseGet(() -> {
            var b = bedService.createBed(new BedRequest("CONS-BED-99B", room.getId(), BedStatus.OCCUPIED));
            return bedRepository.findById(b.getId()).orElseThrow();
        });

        patientA = patientRepository.findByPatientCodeIgnoreCase("CONS-PAT-001")
                .orElseGet(() -> {
                    Patient p = new Patient("CONS-PAT-001", "Patient A", 30, "MALE", "Fever", bedA);
                    p.setUser(patientUserA);
                    return patientRepository.save(p);
                });

        patientB = patientRepository.findByPatientCodeIgnoreCase("CONS-PAT-002")
                .orElseGet(() -> {
                    Patient p = new Patient("CONS-PAT-002", "Patient B", 45, "FEMALE", "Cough", bedB);
                    p.setUser(patientUserB);
                    return patientRepository.save(p);
                });
    }

    @AfterEach
    void tearDown() {
        consultationRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "test.doctor", roles = {"DOCTOR"})
    @DisplayName("Doctor can record a consultation with diagnosis, treatment, and prescription")
    void testDoctorCanRecordConsultation() throws Exception {
        ConsultationRequest request = new ConsultationRequest(
                patientA.getId(),
                null,
                "Acute Bronchitis",
                "Rest, hydration, and bronchodilator",
                "Albuterol inhaler 2 puffs q4h prn, Amoxicillin 500mg tid x 7d",
                "Follow up in one week if symptoms do not improve",
                ConsultationStatus.COMPLETED
        );

        mockMvc.perform(post("/api/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.diagnosis").value("Acute Bronchitis"))
                .andExpect(jsonPath("$.data.prescription").value("Albuterol inhaler 2 puffs q4h prn, Amoxicillin 500mg tid x 7d"));
    }

    @Test
    @WithMockUser(username = "patient.a", roles = {"PATIENT"})
    @DisplayName("Patient can view their own consultation records")
    void testPatientCanViewOwnConsultations() throws Exception {
        // First record a consultation as doctor
        ConsultationRequest request = new ConsultationRequest(
                patientA.getId(),
                null,
                "Viral Pharyngitis",
                "Throat lozenges and warm saline gargle",
                "Paracetamol 500mg tid prn",
                "Routine follow-up",
                ConsultationStatus.COMPLETED
        );

        mockMvc.perform(post("/api/consultations")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test.doctor").roles("DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Patient A views their own consultations -> 200 OK
        mockMvc.perform(get("/api/consultations/patient/" + patientA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].diagnosis").value("Viral Pharyngitis"));
    }

    @Test
    @WithMockUser(username = "patient.b", roles = {"PATIENT"})
    @DisplayName("Patient is forbidden from viewing another patient's consultation records")
    void testPatientCannotViewOtherPatientConsultations() throws Exception {
        // Patient B tries to view Patient A's consultations -> 403 Forbidden
        mockMvc.perform(get("/api/consultations/patient/" + patientA.getId()))
                .andExpect(status().isForbidden());
    }
}
