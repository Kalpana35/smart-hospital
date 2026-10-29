package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.BedRequest;
import com.hospital.smart_hospital.dto.BedResponse;
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
import com.hospital.smart_hospital.service.RoomService;
import com.hospital.smart_hospital.service.impl.BedServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BedServiceTest {

    @Mock
    private BedRepository bedRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private BedServiceImpl bedService;

    private Room sampleRoom;
    private Bed sampleBed;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("R-101", "ICU", "Cardiology");
        sampleRoom.setId(10L);

        sampleBed = new Bed("B-01", sampleRoom, BedStatus.AVAILABLE);
        sampleBed.setId(100L);
    }

    @Test
    @DisplayName("Should create bed successfully in an existing room")
    void testCreateBed_Success() {
        BedRequest request = new BedRequest("B-01", 10L, BedStatus.AVAILABLE);

        when(roomService.getRoomEntity(10L)).thenReturn(sampleRoom);
        when(bedRepository.existsByRoomIdAndBedNumberIgnoreCase(10L, "B-01")).thenReturn(false);
        when(bedRepository.save(any(Bed.class))).thenReturn(sampleBed);

        BedResponse response = bedService.createBed(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getBedNumber()).isEqualTo("B-01");
        assertThat(response.getStatus()).isEqualTo(BedStatus.AVAILABLE);
        assertThat(response.getRoomId()).isEqualTo(10L);
        assertThat(response.getRoomNumber()).isEqualTo("R-101");
        assertThat(response.getRoomType()).isEqualTo("ICU");
        assertThat(response.getWardDepartment()).isEqualTo("Cardiology");

        verify(bedRepository).save(any(Bed.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when bed number already exists within the same room")
    void testCreateBed_DuplicateBedNumberInSameRoom() {
        BedRequest request = new BedRequest("B-01", 10L, BedStatus.AVAILABLE);

        when(roomService.getRoomEntity(10L)).thenReturn(sampleRoom);
        when(bedRepository.existsByRoomIdAndBedNumberIgnoreCase(10L, "B-01")).thenReturn(true);

        assertThatThrownBy(() -> bedService.createBed(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Bed number 'B-01' already exists in Room R-101.");

        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating bed for a non-existent room")
    void testCreateBed_RoomNotFound() {
        BedRequest request = new BedRequest("B-01", 999L, BedStatus.AVAILABLE);

        when(roomService.getRoomEntity(999L))
                .thenThrow(new ResourceNotFoundException("Room not found with ID: 999"));

        assertThatThrownBy(() -> bedService.createBed(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw BadRequestException when bed request has missing fields")
    void testCreateBed_MissingFields() {
        BedRequest missingNumber = new BedRequest("   ", 10L, null);
        assertThatThrownBy(() -> bedService.createBed(missingNumber))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Bed number is required.");

        BedRequest missingRoom = new BedRequest("B-01", null, null);
        assertThatThrownBy(() -> bedService.createBed(missingRoom))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Associated room ID is required.");
    }

    @Test
    @DisplayName("Should update bed status successfully")
    void testUpdateBedStatus_Success() {
        when(bedRepository.findById(100L)).thenReturn(Optional.of(sampleBed));
        when(bedRepository.save(any(Bed.class))).thenReturn(sampleBed);

        BedResponse response = bedService.updateBedStatus(100L, BedStatus.OCCUPIED);

        assertThat(response.getStatus()).isEqualTo(BedStatus.OCCUPIED);
        assertThat(sampleBed.getStatus()).isEqualTo(BedStatus.OCCUPIED);
        verify(bedRepository).save(sampleBed);
    }

    @Test
    @DisplayName("Should filter beds by roomId and status")
    void testGetBeds_FilterByRoomAndStatus() {
        when(bedRepository.findByRoomIdAndStatus(10L, BedStatus.AVAILABLE))
                .thenReturn(List.of(sampleBed));

        List<BedResponse> list = bedService.getBeds(10L, BedStatus.AVAILABLE);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getBedNumber()).isEqualTo("B-01");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating bed number to an existing one in the room")
    void testUpdateBed_DuplicateInRoom() {
        BedRequest updateRequest = new BedRequest("B-02", 10L, BedStatus.MAINTENANCE);

        when(bedRepository.findById(100L)).thenReturn(Optional.of(sampleBed));
        when(roomService.getRoomEntity(10L)).thenReturn(sampleRoom);
        when(bedRepository.existsByRoomIdAndBedNumberIgnoreCaseAndIdNot(10L, "B-02", 100L))
                .thenReturn(true);

        assertThatThrownBy(() -> bedService.updateBed(100L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in use in Room R-101");
    }

    @Test
    @DisplayName("Should prevent changing OCCUPIED bed to MAINTENANCE when active patient is admitted")
    void testUpdateBedStatus_PreventMaintenanceWhenOccupiedByActivePatient() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        Patient activePatient = new Patient("PAT-101", "John Doe", 45, "MALE", "Cardiac Arrest", sampleBed);

        when(bedRepository.findById(100L)).thenReturn(Optional.of(sampleBed));
        when(patientRepository.findByAssignedBedIdAndStatus(100L, PatientStatus.ADMITTED))
                .thenReturn(Optional.of(activePatient));

        assertThatThrownBy(() -> bedService.updateBedStatus(100L, BedStatus.MAINTENANCE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change status of Bed 'B-01' to MAINTENANCE")
                .hasMessageContaining("Patient John Doe (PAT-101) is currently admitted to this bed. Discharge the patient first.");

        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    @DisplayName("Should prevent changing OCCUPIED bed to AVAILABLE when active patient is admitted")
    void testUpdateBedStatus_PreventAvailableWhenOccupiedByActivePatient() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        Patient activePatient = new Patient("PAT-102", "Jane Smith", 32, "FEMALE", "Observation", sampleBed);

        when(bedRepository.findById(100L)).thenReturn(Optional.of(sampleBed));
        when(patientRepository.findByAssignedBedIdAndStatus(100L, PatientStatus.ADMITTED))
                .thenReturn(Optional.of(activePatient));

        assertThatThrownBy(() -> bedService.updateBedStatus(100L, BedStatus.AVAILABLE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change status of Bed 'B-01' to AVAILABLE")
                .hasMessageContaining("Patient Jane Smith (PAT-102) is currently admitted to this bed. Discharge the patient first.");

        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    @DisplayName("Should prevent updating bed status to MAINTENANCE via updateBed when active patient is admitted")
    void testUpdateBed_PreventStatusChangeWhenOccupiedByActivePatient() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        Patient activePatient = new Patient("PAT-101", "John Doe", 45, "MALE", "Cardiac Arrest", sampleBed);
        BedRequest updateRequest = new BedRequest("B-01", 10L, BedStatus.MAINTENANCE);

        when(bedRepository.findById(100L)).thenReturn(Optional.of(sampleBed));
        when(roomService.getRoomEntity(10L)).thenReturn(sampleRoom);
        when(bedRepository.existsByRoomIdAndBedNumberIgnoreCaseAndIdNot(10L, "B-01", 100L)).thenReturn(false);
        when(patientRepository.findByAssignedBedIdAndStatus(100L, PatientStatus.ADMITTED))
                .thenReturn(Optional.of(activePatient));

        assertThatThrownBy(() -> bedService.updateBed(100L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change status of Bed 'B-01' to MAINTENANCE")
                .hasMessageContaining("Patient John Doe (PAT-101) is currently admitted to this bed. Discharge the patient first.");

        verify(bedRepository, never()).save(any(Bed.class));
    }
}

