package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.DepartmentResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Department;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.DepartmentRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    public DepartmentServiceImpl(
            DepartmentRepository departmentRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        List<Bed> allBeds = bedRepository.findAll();

        List<DepartmentResponse> responses = new ArrayList<>();
        for (Department dept : departments) {
            List<Room> deptRooms = allRooms.stream()
                    .filter(r -> (r.getDepartment() != null && r.getDepartment().getId().equals(dept.getId()))
                            || (r.getWardDepartment() != null && r.getWardDepartment().equalsIgnoreCase(dept.getName())))
                    .toList();

            List<Long> roomIds = deptRooms.stream().map(Room::getId).toList();
            List<Bed> deptBeds = allBeds.stream()
                    .filter(b -> b.getRoom() != null && roomIds.contains(b.getRoom().getId()))
                    .toList();

            int total = deptBeds.size();
            int avail = (int) deptBeds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
            int occ = (int) deptBeds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
            int dirty = (int) deptBeds.stream().filter(b -> b.getStatus() == BedStatus.DIRTY).count();

            responses.add(new DepartmentResponse(
                    dept.getId(),
                    dept.getName(),
                    dept.getCode(),
                    dept.getDescription(),
                    dept.getHeadOfDepartment(),
                    deptRooms.size(),
                    total,
                    avail,
                    occ,
                    dirty
            ));
        }

        return responses;
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));

        List<Room> deptRooms = roomRepository.findByDepartmentId(dept.getId());
        List<Long> roomIds = deptRooms.stream().map(Room::getId).toList();
        List<Bed> deptBeds = bedRepository.findAll().stream()
                .filter(b -> b.getRoom() != null && roomIds.contains(b.getRoom().getId()))
                .toList();

        int total = deptBeds.size();
        int avail = (int) deptBeds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
        int occ = (int) deptBeds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
        int dirty = (int) deptBeds.stream().filter(b -> b.getStatus() == BedStatus.DIRTY).count();

        return new DepartmentResponse(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getDescription(),
                dept.getHeadOfDepartment(),
                deptRooms.size(),
                total,
                avail,
                occ,
                dirty
        );
    }

    @Override
    @Transactional
    public Department createDepartment(Department department) {
        if (department == null || department.getName() == null || department.getName().trim().isEmpty()) {
            throw new BadRequestException("Department name is required.");
        }
        if (department.getCode() == null || department.getCode().trim().isEmpty()) {
            throw new BadRequestException("Department code is required.");
        }
        if (departmentRepository.existsByCodeIgnoreCase(department.getCode().trim())) {
            throw new DuplicateResourceException("Department code '" + department.getCode().trim() + "' already exists.");
        }

        department.setName(department.getName().trim());
        department.setCode(department.getCode().trim().toUpperCase());
        return departmentRepository.save(department);
    }
}
