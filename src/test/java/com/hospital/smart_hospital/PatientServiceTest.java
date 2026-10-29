package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.PatientResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.PatientStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.impl.PatientServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private BedService bedService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Room sampleRoom;
    private Bed sampleBed;
    private Patient samplePatient;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("R-101", "ICU", "Critical Care");
        sampleRoom.setId(1L);

        sampleBed = new Bed("BED-1", sampleRoom, BedStatus.AVAILABLE);
        sampleBed.setId(10L);

        samplePatient = new Patient("PAT-001", "Alice Smith", 35, "FEMALE", "Pneumonia", sampleBed);
        samplePatient.setId(100L);
    }

    @Test
    @DisplayName("Should successfully admit patient to an AVAILABLE bed and set bed to OCCUPIED")
    void testAdmitPatient_Success() {
        PatientAdmissionRequest request = new PatientAdmissionRequest(
                "PAT-001", "Alice Smith", 35, "FEMALE", "Pneumonia", 10L
        );

        when(patientRepository.existsByPatientCodeIgnoreCase("PAT-001")).thenReturn(false);
        when(bedService.getBedEntity(10L)).thenReturn(sampleBed);
        when(patientRepository.existsByAssignedBedIdAndStatus(10L, PatientStatus.ADMITTED)).thenReturn(false);
        when(bedRepository.save(any(Bed.class))).thenReturn(sampleBed);
        when(patientRepository.save(any(Patient.class))).thenReturn(samplePatient);

        PatientResponse response = patientService.admitPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getPatientCode()).isEqualTo("PAT-001");
        assertThat(response.getStatus()).isEqualTo(PatientStatus.ADMITTED);
        assertThat(response.getRoomType()).isEqualTo("ICU");
        assertThat(response.getRoomNumber()).isEqualTo("R-101");
        assertThat(sampleBed.getStatus()).isEqualTo(BedStatus.OCCUPIED);

        verify(bedRepository).save(sampleBed);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should reject admission when bed is already OCCUPIED")
    void testAdmitPatient_OccupiedBedRejection() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        PatientAdmissionRequest request = new PatientAdmissionRequest(
                "PAT-002", "Bob Johnson", 40, "MALE", "Fracture", 10L
        );

        when(patientRepository.existsByPatientCodeIgnoreCase("PAT-002")).thenReturn(false);
        when(bedService.getBedEntity(10L)).thenReturn(sampleBed);

        assertThatThrownBy(() -> patientService.admitPatient(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already OCCUPIED");

        verify(bedRepository, never()).save(any(Bed.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should reject admission when bed is under MAINTENANCE")
    void testAdmitPatient_MaintenanceBedRejection() {
        sampleBed.setStatus(BedStatus.MAINTENANCE);
        PatientAdmissionRequest request = new PatientAdmissionRequest(
                "PAT-003", "Charlie Brown", 50, "MALE", "Observation", 10L
        );

        when(patientRepository.existsByPatientCodeIgnoreCase("PAT-003")).thenReturn(false);
        when(bedService.getBedEntity(10L)).thenReturn(sampleBed);

        assertThatThrownBy(() -> patientService.admitPatient(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("under MAINTENANCE");

        verify(bedRepository, never()).save(any(Bed.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should reject duplicate patient code")
    void testAdmitPatient_DuplicatePatientCode() {
        PatientAdmissionRequest request = new PatientAdmissionRequest(
                "PAT-001", "Alice Duplicate", 35, "FEMALE", "Flu", 10L
        );

        when(patientRepository.existsByPatientCodeIgnoreCase("PAT-001")).thenReturn(true);

        assertThatThrownBy(() -> patientService.admitPatient(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Patient with code 'PAT-001' already exists.");

        verify(bedService, never()).getBedEntity(any());
    }

    @Test
    @DisplayName("Should successfully discharge patient, record discharge date, and make bed AVAILABLE")
    void testDischargePatient_Success() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        samplePatient.setStatus(PatientStatus.ADMITTED);

        when(patientRepository.findById(100L)).thenReturn(Optional.of(samplePatient));
        when(bedRepository.save(sampleBed)).thenReturn(sampleBed);
        when(patientRepository.save(samplePatient)).thenReturn(samplePatient);

        PatientResponse response = patientService.dischargePatient(100L);

        assertThat(response.getStatus()).isEqualTo(PatientStatus.DISCHARGED);
        assertThat(samplePatient.getDischargeDate()).isNotNull();
        assertThat(sampleBed.getStatus()).isEqualTo(BedStatus.AVAILABLE);

        verify(bedRepository).save(sampleBed);
        verify(patientRepository).save(samplePatient);
    }

    @Test
    @DisplayName("Should prevent repeated discharge of an already discharged patient")
    void testDischargePatient_RepeatedDischarge() {
        samplePatient.setStatus(PatientStatus.DISCHARGED);
        samplePatient.setDischargeDate(LocalDateTime.now().minusHours(1));

        when(patientRepository.findById(100L)).thenReturn(Optional.of(samplePatient));

        assertThatThrownBy(() -> patientService.dischargePatient(100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is already discharged.");

        verify(bedRepository, never()).save(any(Bed.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when patient ID does not exist")
    void testDischargePatient_NotFound() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.dischargePatient(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with ID: 999");
    }
}

