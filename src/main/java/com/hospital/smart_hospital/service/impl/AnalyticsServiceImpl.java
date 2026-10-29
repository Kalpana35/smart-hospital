package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.HospitalAnalyticsResponse;
import com.hospital.smart_hospital.dto.RoomTypeAnalyticsDto;
import com.hospital.smart_hospital.dto.WardAnalyticsDto;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.PatientStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
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

    public AnalyticsServiceImpl(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PatientRepository patientRepository
    ) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.patientRepository = patientRepository;
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

        for (Bed b : beds) {
            if (b.getStatus() == BedStatus.AVAILABLE) {
                availableBeds++;
            } else if (b.getStatus() == BedStatus.OCCUPIED) {
                occupiedBeds++;
            } else if (b.getStatus() == BedStatus.MAINTENANCE) {
                maintenanceBeds++;
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
            double wRate = wTotalBeds > 0 ? Math.round((wOcc * 1000.0) / wTotalBeds) / 10.0 : 0.0;

            wardList.add(new WardAnalyticsDto(wardName, wardRooms.size(), wTotalBeds, wAvail, wOcc, wMaint, wRate));
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

        return new HospitalAnalyticsResponse(
                totalRooms,
                totalBeds,
                availableBeds,
                occupiedBeds,
                maintenanceBeds,
                activePatients,
                dischargedPatients,
                overallOccupancyRate,
                avgLengthOfStayHours,
                wardList,
                roomTypeList
        );
    }
}
