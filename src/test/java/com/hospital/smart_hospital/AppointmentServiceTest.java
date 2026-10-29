package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.AppointmentBookingRequest;
import com.hospital.smart_hospital.dto.AppointmentResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.model.Appointment;
import com.hospital.smart_hospital.model.AppointmentStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.impl.AppointmentServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private User doctorUser;
    private User patientUser;
    private Patient patient;
    private Authentication patientAuth;
    private Authentication doctorAuth;

    @BeforeEach
    void setUp() {
        doctorUser = new User("dr.smith", "password", "Dr. Jane Smith", Role.ROLE_DOCTOR);
        doctorUser.setId(10L);

        patientUser = new User("alice.patient", "password", "Alice Smith", Role.ROLE_PATIENT);
        patientUser.setId(20L);

        patient = new Patient("PAT-001", "Alice Smith", 40, "FEMALE", "Sepsis", null);
        patient.setId(100L);
        patient.setUser(patientUser);

        patientAuth = new TestingAuthenticationToken("alice.patient", "password", "ROLE_PATIENT");
        doctorAuth = new TestingAuthenticationToken("dr.smith", "password", "ROLE_DOCTOR");
    }

    @Test
    @DisplayName("Should successfully book appointment when requested by authenticated patient")
    void testBookAppointment_Success() {
        LocalDateTime apptTime = LocalDateTime.now().plusDays(2);
        AppointmentBookingRequest request = new AppointmentBookingRequest(
                null, 10L, apptTime, "Cardiology", "Follow-up check"
        );

        when(patientRepository.findByUserUsernameIgnoreCase("alice.patient")).thenReturn(Optional.of(patient));
        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(appointmentRepository.existsByDoctorIdAndStatusAndAppointmentDateTimeBetween(
                eq(10L), eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        Appointment savedAppointment = new Appointment(patient, doctorUser, apptTime, "Cardiology", "Follow-up check");
        savedAppointment.setId(1L);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        AppointmentResponse response = appointmentService.bookAppointment(request, patientAuth);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDoctorName()).isEqualTo("Dr. Jane Smith");
        assertThat(response.getPatientCode()).isEqualTo("PAT-001");
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should reject appointment booking when doctor has a schedule conflict within 30 minutes")
    void testBookAppointment_ConflictRejected() {
        LocalDateTime apptTime = LocalDateTime.now().plusDays(2);
        AppointmentBookingRequest request = new AppointmentBookingRequest(
                null, 10L, apptTime, "Cardiology", "Routine consultation"
        );

        when(patientRepository.findByUserUsernameIgnoreCase("alice.patient")).thenReturn(Optional.of(patient));
        when(userRepository.findById(10L)).thenReturn(Optional.of(doctorUser));
        when(appointmentRepository.existsByDoctorIdAndStatusAndAppointmentDateTimeBetween(
                eq(10L), eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, patientAuth))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has an active appointment scheduled around this time");
    }

    @Test
    @DisplayName("Should allow patient to cancel their own scheduled appointment")
    void testCancelAppointment_SuccessByOwner() {
        LocalDateTime apptTime = LocalDateTime.now().plusDays(1);
        Appointment appointment = new Appointment(patient, doctorUser, apptTime, "Cardiology", "Checkup");
        appointment.setId(5L);

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(5L, "Cannot attend", patientAuth);

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getNotes()).contains("Cancellation Reason: Cannot attend");
    }

    @Test
    @DisplayName("Should reject cancellation when attempted by an unauthorized patient")
    void testCancelAppointment_ForbiddenForDifferentPatient() {
        LocalDateTime apptTime = LocalDateTime.now().plusDays(1);
        Appointment appointment = new Appointment(patient, doctorUser, apptTime, "Cardiology", "Checkup");
        appointment.setId(5L);

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));

        Authentication intruderAuth = new TestingAuthenticationToken("bob.intruder", "password", "ROLE_PATIENT");

        assertThatThrownBy(() -> appointmentService.cancelAppointment(5L, "Sabotage", intruderAuth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You cannot cancel appointments of other patients");
    }

    @Test
    @DisplayName("Should allow assigned doctor to complete consultation with clinical notes")
    void testCompleteAppointment_SuccessByDoctor() {
        LocalDateTime apptTime = LocalDateTime.now().minusHours(1);
        Appointment appointment = new Appointment(patient, doctorUser, apptTime, "Cardiology", "Checkup");
        appointment.setId(5L);

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = appointmentService.completeAppointment(5L, "Vitals normal. Prescribed rest.", doctorAuth);

        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getNotes()).contains("Doctor Consultation Note: Vitals normal. Prescribed rest.");
    }
}
