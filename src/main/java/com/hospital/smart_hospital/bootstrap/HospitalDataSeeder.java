package com.hospital.smart_hospital.bootstrap;

import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Department;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.DepartmentRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class HospitalDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(HospitalDataSeeder.class);

    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    public HospitalDataSeeder(
            DepartmentRepository departmentRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed or retrieve the 5 core hospital departments
        Department icu = getOrCreateDepartment("ICU", "ICU", "Intensive Care Unit with high-acuity monitoring and ventilation", "Dr. Robert Vance");
        Department gen = getOrCreateDepartment("General Ward", "GEN", "Inpatient medical and general recovery care ward", "Dr. Sarah Jenkins");
        Department surg = getOrCreateDepartment("Surgical Ward", "SURG", "Pre-operative preparation and post-surgical recovery", "Dr. David Miller");
        Department iso = getOrCreateDepartment("Isolation Unit", "ISO", "Negative pressure suites for infectious disease isolation", "Dr. Linda Chang");
        Department emerg = getOrCreateDepartment("Emergency Department", "EMERG", "Rapid triage, acute resuscitation, and observation", "Dr. Marcus Webb");

        // 2. Link any legacy unassigned rooms to departments
        List<Room> existingRooms = roomRepository.findAll();
        for (Room r : existingRooms) {
            if (r.getDepartment() == null) {
                String ward = r.getWardDepartment() != null ? r.getWardDepartment().toLowerCase() : "";
                if (ward.contains("icu") || ward.contains("critical")) {
                    r.setDepartment(icu);
                    r.setHasIcuMonitoring(true);
                } else if (ward.contains("surg")) {
                    r.setDepartment(surg);
                } else if (ward.contains("iso")) {
                    r.setDepartment(iso);
                    r.setHasIsolation(true);
                } else if (ward.contains("emerg")) {
                    r.setDepartment(emerg);
                } else {
                    r.setDepartment(gen);
                }
                roomRepository.save(r);
            }
        }

        // 3. Check if 100 beds are already present
        long currentBedCount = bedRepository.count();
        if (currentBedCount >= 100) {
            logger.info("HospitalDataSeeder: Initial bed capacity already seeded. Total beds: {}", currentBedCount);
            return;
        }

        logger.info("HospitalDataSeeder: Current bed count is {}. Seeding prototype 100-bed hospital dataset across 5 departments...", currentBedCount);

        // 4. Seed Department 1: ICU (20 beds across 15 rooms: 10 single rooms + 5 double rooms)
        seedDepartmentRooms(icu, "ICU", 101, 110, 1, 1200.0, true, false, "VENTILATOR,ICU_MONITOR,OXYGEN");
        seedDepartmentRooms(icu, "ICU-Stepdown", 111, 115, 2, 950.0, true, false, "CARDIAC_MONITOR,OXYGEN");

        // 5. Seed Department 2: General Ward (30 beds across 10 rooms: 5 double rooms + 5 four-bed bays)
        seedDepartmentRooms(gen, "General", 201, 205, 2, 280.0, false, false, "STANDARD");
        seedDepartmentRooms(gen, "General", 206, 210, 4, 220.0, false, false, "STANDARD");

        // 6. Seed Department 3: Surgical Ward (25 beds across 15 rooms: 5 single post-op + 10 double rooms)
        seedDepartmentRooms(surg, "Surgical", 301, 305, 1, 650.0, false, false, "POST_OP_MONITOR,OXYGEN");
        seedDepartmentRooms(surg, "Surgical", 306, 315, 2, 550.0, false, false, "SURGICAL_MONITOR");

        // 7. Seed Department 4: Isolation Unit (10 beds across 10 single negative-pressure suites)
        seedDepartmentRooms(iso, "Isolation", 401, 410, 1, 850.0, false, true, "NEGATIVE_PRESSURE,HEPA_FILTER");

        // 8. Seed Department 5: Emergency Department (15 beds across 10 rooms: 5 single triage + 5 double observation)
        seedDepartmentRooms(emerg, "Emergency", 501, 505, 1, 500.0, false, false, "TRIAGE_RESUSCITATION,OXYGEN");
        seedDepartmentRooms(emerg, "Emergency", 506, 510, 2, 420.0, false, false, "ACUTE_OBSERVATION");

        logger.info("HospitalDataSeeder: Completed seeding. Total hospital beds now in system: {}", bedRepository.count());
    }

    private Department getOrCreateDepartment(String name, String code, String description, String head) {
        return departmentRepository.findByCodeIgnoreCase(code).orElseGet(() -> {
            Department d = new Department(name, code, description, head);
            return departmentRepository.save(d);
        });
    }

    private void seedDepartmentRooms(
            Department dept,
            String roomType,
            int startNum,
            int endNum,
            int bedsPerRoom,
            Double dailyRate,
            boolean icuMonitoring,
            boolean isolation,
            String features
    ) {
        for (int i = startNum; i <= endNum; i++) {
            String roomNumber = dept.getCode() + "-" + i;
            Room room = roomRepository.findByRoomNumberIgnoreCase(roomNumber).orElseGet(() -> {
                Room newRoom = new Room(roomNumber, roomType, dept.getName(), dept, dailyRate, icuMonitoring, isolation);
                return roomRepository.save(newRoom);
            });

            for (int b = 1; b <= bedsPerRoom; b++) {
                String bedNumber = "B-" + b;
                if (!bedRepository.existsByRoomIdAndBedNumberIgnoreCase(room.getId(), bedNumber)) {
                    Bed bed = new Bed(bedNumber, room, BedStatus.AVAILABLE, dailyRate, features);
                    bedRepository.save(bed);
                }
            }
        }
    }
}
