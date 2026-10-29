package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.AppointmentBookingRequest;
import com.hospital.smart_hospital.dto.AppointmentStatusUpdateRequest;
import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.PatientSelfRegistrationRequest;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.PatientService;
import com.hospital.smart_hospital.service.RoomService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AppointmentIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BedService bedService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User doctorUser;
    private MockHttpSession patientSession;
    private MockHttpSession doctorSession;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Ensure doctor user exists
        doctorUser = userRepository.findByUsernameIgnoreCase("dr.appointment.test")
                .orElseGet(() -> userRepository.save(new User(
                        "dr.appointment.test",
                        passwordEncoder.encode("DoctorPass@123"),
                        "Dr. Sarah Specialist",
                        Role.ROLE_DOCTOR
                )));

        // Create Room, Bed & Patient
        var room = roomService.createRoom(new RoomRequest("APPT-ROOM", "STANDARD", "Outpatient"));
        var bed = bedService.createBed(new BedRequest("APPT-BED", room.getId(), BedStatus.AVAILABLE));

        var patient = patientService.admitPatient(new PatientAdmissionRequest(
                "PAT-APPT-01", "Charlie Brown", 28, "MALE", "Consultation", bed.getId()
        ));

        // Register patient user
        mockMvc.perform(post("/api/auth/register-patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PatientSelfRegistrationRequest(
                                "charlie.patient", "Secret@123", "Charlie Brown", "PAT-APPT-01"
                        ))))
                .andExpect(status().isCreated());

        // Login as patient
        var patientLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("charlie.patient", "Secret@123"))))
                .andExpect(status().isOk())
                .andReturn();
        patientSession = (MockHttpSession) patientLoginRes.getRequest().getSession();

        // Login as doctor
        var doctorLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("dr.appointment.test", "DoctorPass@123"))))
                .andExpect(status().isOk())
                .andReturn();
        doctorSession = (MockHttpSession) doctorLoginRes.getRequest().getSession();
    }

    @Test
    @DisplayName("Complete Appointment Booking, Schedule Conflict, Consultation Completion & Cancellation Flow")
    void testAppointmentLifecycle() throws Exception {
        LocalDateTime apptTime = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);

        // 1. Patient books appointment with Dr. Sarah Specialist
        AppointmentBookingRequest bookingReq = new AppointmentBookingRequest(
                null, doctorUser.getId(), apptTime, "Cardiology", "Chest discomfort check"
        );

        String resStr = mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.doctorName", is("Dr. Sarah Specialist")))
                .andExpect(jsonPath("$.data.status", is("SCHEDULED")))
                .andReturn().getResponse().getContentAsString();

        long appointmentId = objectMapper.readTree(resStr).path("data").path("id").asLong();

        // 2. Reject booking for another appointment with same doctor within 30 min
        AppointmentBookingRequest conflictReq = new AppointmentBookingRequest(
                null, doctorUser.getId(), apptTime.plusMinutes(15), "Cardiology", "Second check"
        );
        mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already has an active appointment scheduled around this time")));

        // 3. Patient views my appointments
        mockMvc.perform(get("/api/appointments/my").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is((int) appointmentId)));

        // 4. Doctor views appointments and marks completed
        AppointmentStatusUpdateRequest completeReq = new AppointmentStatusUpdateRequest(
                "ECG normal. Recommended hydration.", null
        );
        mockMvc.perform(patch("/api/appointments/" + appointmentId + "/complete")
                        .session(doctorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.notes", containsString("ECG normal")));

        // 5. Patient books a second appointment for next week and cancels it
        LocalDateTime appt2Time = LocalDateTime.now().plusDays(7);
        AppointmentBookingRequest booking2 = new AppointmentBookingRequest(
                null, doctorUser.getId(), appt2Time, "General Medicine", "Routine labs"
        );
        String res2Str = mockMvc.perform(post("/api/appointments")
                        .session(patientSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long appt2Id = objectMapper.readTree(res2Str).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/appointments/" + appt2Id + "/cancel")
                        .session(patientSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentStatusUpdateRequest(null, "Schedule conflict"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }
}
