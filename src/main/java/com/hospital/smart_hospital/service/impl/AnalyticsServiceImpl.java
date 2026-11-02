package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.HospitalAnalyticsResponse;
import com.hospital.smart_hospital.dto.RoomTypeAnalyticsDto;
import com.hospital.smart_hospital.dto.WardAnalyticsDto;
import com.hospital.smart_hospital.model.AppointmentStatus;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedCleaningLog;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.PatientStatus;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.BedCleaningLogRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.DepartmentRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.AnalyticsService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final DepartmentRepository departmentRepository;
    private final BedCleaningLogRepository bedCleaningLogRepository;

    public AnalyticsServiceImpl(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository
    ) {
        this(roomRepository, bedRepository, patientRepository, userRepository, appointmentRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AnalyticsServiceImpl(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            DepartmentRepository departmentRepository,
            BedCleaningLogRepository bedCleaningLogRepository
    ) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.departmentRepository = departmentRepository;
        this.bedCleaningLogRepository = bedCleaningLogRepository;
    }

    @Override
    public HospitalAnalyticsResponse getHospitalAnalytics() {
        List<Room> rooms = roomRepository.findAll();
        List<Bed> beds = bedRepository.findAll();
        List<Patient> patients = patientRepository.findAll();

        int totalRooms = rooms.size();
        int totalBeds = beds.size();

        int availableBeds = 0;
        int occupiedBeds = 0;
        int maintenanceBeds = 0;
        int dirtyBeds = 0;

        for (Bed b : beds) {
            if (b.getStatus() == BedStatus.AVAILABLE) {
                availableBeds++;
            } else if (b.getStatus() == BedStatus.OCCUPIED) {
                occupiedBeds++;
            } else if (b.getStatus() == BedStatus.MAINTENANCE) {
                maintenanceBeds++;
            } else if (b.getStatus() == BedStatus.DIRTY) {
                dirtyBeds++;
            }
        }

        int activePatients = 0;
        int dischargedPatients = 0;
        double totalStayHours = 0.0;
        int staysCounted = 0;

        for (Patient p : patients) {
            if (p.getStatus() == PatientStatus.ADMITTED) {
                activePatients++;
            } else if (p.getStatus() == PatientStatus.DISCHARGED) {
                dischargedPatients++;
                if (p.getAdmissionDate() != null && p.getDischargeDate() != null) {
                    long minutes = Duration.between(p.getAdmissionDate(), p.getDischargeDate()).toMinutes();
                    totalStayHours += Math.max(0.1, minutes / 60.0);
                    staysCounted++;
                }
            }
        }

        double overallOccupancyRate = totalBeds > 0
                ? Math.round((occupiedBeds * 1000.0) / totalBeds) / 10.0
                : 0.0;

        double avgLengthOfStayHours = staysCounted > 0
                ? Math.round((totalStayHours * 10.0) / staysCounted) / 10.0
                : 0.0;

        // Ward breakdown
        Map<String, List<Room>> roomsByWard = rooms.stream()
                .collect(Collectors.groupingBy(r -> r.getWardDepartment() != null && !r.getWardDepartment().trim().isEmpty()
                        ? r.getWardDepartment().trim()
                        : "General"));

        List<WardAnalyticsDto> wardList = new ArrayList<>();
        for (Map.Entry<String, List<Room>> entry : roomsByWard.entrySet()) {
            String wardName = entry.getKey();
            List<Room> wardRooms = entry.getValue();
            List<Long> wardRoomIds = wardRooms.stream().map(Room::getId).toList();

            List<Bed> wardBeds = beds.stream()
                    .filter(b -> b.getRoom() != null && wardRoomIds.contains(b.getRoom().getId()))
                    .toList();

            int wTotalBeds = wardBeds.size();
            int wAvail = (int) wardBeds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
            int wOcc = (int) wardBeds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
            int wMaint = (int) wardBeds.stream().filter(b -> b.getStatus() == BedStatus.MAINTENANCE).count();
            int wDirty = (int) wardBeds.stream().filter(b -> b.getStatus() == BedStatus.DIRTY).count();
            double wRate = wTotalBeds > 0 ? Math.round((wOcc * 1000.0) / wTotalBeds) / 10.0 : 0.0;

            wardList.add(new WardAnalyticsDto(wardName, wardRooms.size(), wTotalBeds, wAvail, wOcc, wMaint, wDirty, wRate));
        }
        wardList.sort(Comparator.comparing(WardAnalyticsDto::getWardName, String.CASE_INSENSITIVE_ORDER));

        // Room type breakdown
        Map<String, List<Bed>> bedsByRoomType = beds.stream()
                .collect(Collectors.groupingBy(b -> (b.getRoom() != null && b.getRoom().getRoomType() != null && !b.getRoom().getRoomType().trim().isEmpty())
                        ? b.getRoom().getRoomType().trim()
                        : "Standard"));

        List<RoomTypeAnalyticsDto> roomTypeList = new ArrayList<>();
        for (Map.Entry<String, List<Bed>> entry : bedsByRoomType.entrySet()) {
            String typeName = entry.getKey();
            List<Bed> typeBeds = entry.getValue();

            int tTotal = typeBeds.size();
            int tAvail = (int) typeBeds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
            int tOcc = (int) typeBeds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
            int tMaint = (int) typeBeds.stream().filter(b -> b.getStatus() == BedStatus.MAINTENANCE).count();
            double tRate = tTotal > 0 ? Math.round((tOcc * 1000.0) / tTotal) / 10.0 : 0.0;

            roomTypeList.add(new RoomTypeAnalyticsDto(typeName, tTotal, tAvail, tOcc, tMaint, tRate));
        }
        roomTypeList.sort(Comparator.comparing(RoomTypeAnalyticsDto::getRoomType, String.CASE_INSENSITIVE_ORDER));

        int totalDoctors = (int) userRepository.countByRole(Role.ROLE_DOCTOR);
        int totalReceptionists = (int) userRepository.countByRole(Role.ROLE_RECEPTIONIST);
        int totalAppointments = (int) appointmentRepository.count();
        int scheduledAppointments = (int) appointmentRepository.countByStatus(AppointmentStatus.SCHEDULED);
        int completedAppointments = (int) appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);
        int cancelledAppointments = (int) appointmentRepository.countByStatus(AppointmentStatus.CANCELLED);

        int totalDepartments = departmentRepository != null ? (int) departmentRepository.count() : wardList.size();
        int queueLength = bedCleaningLogRepository != null
                ? bedCleaningLogRepository.findByStatusInOrderByPriorityAscDirtyAtAsc(List.of("DIRTY", "IN_PROGRESS")).size()
                : dirtyBeds;
        double avgTurnaround = 25.0;
        if (bedCleaningLogRepository != null) {
            avgTurnaround = bedCleaningLogRepository.findTop50ByStatusOrderByCleanedAtDesc("COMPLETED").stream()
                    .filter(l -> l.getDurationMinutes() != null && l.getDurationMinutes() > 0)
                    .mapToLong(BedCleaningLog::getDurationMinutes)
                    .average()
                    .orElse(25.0);
        }

        HospitalAnalyticsResponse resp = new HospitalAnalyticsResponse(
                totalRooms,
                totalBeds,
                availableBeds,
                occupiedBeds,
                maintenanceBeds,
                activePatients,
                dischargedPatients,
                overallOccupancyRate,
                avgLengthOfStayHours,
                totalDoctors,
                totalReceptionists,
                totalAppointments,
                scheduledAppointments,
                completedAppointments,
                cancelledAppointments,
                wardList,
                roomTypeList
        );
        resp.setDirtyBeds(dirtyBeds);
        resp.setTotalDepartments(totalDepartments > 0 ? totalDepartments : 5);
        resp.setCleaningQueueLength(queueLength);
        resp.setAvgCleaningTurnaroundMinutes(Math.round(avgTurnaround * 10.0) / 10.0);
        return resp;
    }
}
