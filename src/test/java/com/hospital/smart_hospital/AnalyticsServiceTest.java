package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.HospitalAnalyticsResponse;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.PatientStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.impl.AnalyticsServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private Room room1;
    private Room room2;
    private Bed bed1;
    private Bed bed2;
    private Bed bed3;
    private Patient admittedPatient;
    private Patient dischargedPatient;

    @BeforeEach
    void setUp() {
        room1 = new Room("R-101", "ICU", "Critical Care");
        room1.setId(1L);

        room2 = new Room("R-201", "GENERAL", "General Ward");
        room2.setId(2L);

        bed1 = new Bed("BED-1", room1, BedStatus.OCCUPIED);
        bed1.setId(10L);

        bed2 = new Bed("BED-2", room1, BedStatus.AVAILABLE);
        bed2.setId(11L);

        bed3 = new Bed("BED-3", room2, BedStatus.MAINTENANCE);
        bed3.setId(12L);

        admittedPatient = new Patient("PAT-001", "Alice Smith", 40, "FEMALE", "Sepsis", bed1);
        admittedPatient.setStatus(PatientStatus.ADMITTED);
        admittedPatient.setAdmissionDate(LocalDateTime.now().minusHours(24));

        dischargedPatient = new Patient("PAT-002", "Bob Jones", 55, "MALE", "Fracture", null);
        dischargedPatient.setStatus(PatientStatus.DISCHARGED);
        dischargedPatient.setAdmissionDate(LocalDateTime.now().minusHours(48));
        dischargedPatient.setDischargeDate(LocalDateTime.now().minusHours(24)); // 24 hours stay
    }

    @Test
    @DisplayName("Should correctly calculate hospital occupancy and ward analytics")
    void testGetHospitalAnalytics_Calculations() {
        when(roomRepository.findAll()).thenReturn(List.of(room1, room2));
        when(bedRepository.findAll()).thenReturn(List.of(bed1, bed2, bed3));
        when(patientRepository.findAll()).thenReturn(List.of(admittedPatient, dischargedPatient));

        HospitalAnalyticsResponse response = analyticsService.getHospitalAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRooms()).isEqualTo(2);
        assertThat(response.getTotalBeds()).isEqualTo(3);
        assertThat(response.getOccupiedBeds()).isEqualTo(1);
        assertThat(response.getAvailableBeds()).isEqualTo(1);
        assertThat(response.getMaintenanceBeds()).isEqualTo(1);
        assertThat(response.getActivePatients()).isEqualTo(1);
        assertThat(response.getDischargedPatients()).isEqualTo(1);

        // Occupancy rate: 1 occupied out of 3 = 33.3%
        assertThat(response.getOverallOccupancyRate()).isEqualTo(33.3);

        // ALOS: 24.0 hours for the 1 discharged patient
        assertThat(response.getAverageLengthOfStayHours()).isEqualTo(24.0);

        // Ward breakdown checks
        assertThat(response.getWardBreakdown()).hasSize(2);
        assertThat(response.getWardBreakdown().get(0).getWardName()).isEqualTo("Critical Care");
        assertThat(response.getWardBreakdown().get(0).getOccupancyRate()).isEqualTo(50.0); // 1 / 2 beds in room1
    }
}
