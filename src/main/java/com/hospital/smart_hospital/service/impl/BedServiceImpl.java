package com.hospital.smart_hospital.service.impl;

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
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.RoomService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BedServiceImpl implements BedService {

    private final BedRepository bedRepository;
    private final RoomService roomService;
    private final PatientRepository patientRepository;

    public BedServiceImpl(
            BedRepository bedRepository,
            RoomService roomService,
            PatientRepository patientRepository
    ) {
        this.bedRepository = bedRepository;
        this.roomService = roomService;
        this.patientRepository = patientRepository;
    }

    @Override
    public BedResponse createBed(BedRequest request) {
        validateBedRequest(request);

        Room room = roomService.getRoomEntity(request.getRoomId());
        String trimmedBedNumber = request.getBedNumber().trim();

        if (bedRepository.existsByRoomIdAndBedNumberIgnoreCase(room.getId(), trimmedBedNumber)) {
            throw new DuplicateResourceException(
                    "Bed number '" + trimmedBedNumber + "' already exists in Room " + room.getRoomNumber() + "."
            );
        }

        BedStatus status = request.getStatus() != null ? request.getStatus() : BedStatus.AVAILABLE;
        Bed bed = new Bed(trimmedBedNumber, room, status);
        Bed saved = bedRepository.save(bed);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BedResponse getBedById(Long id) {
        Bed bed = getBedEntity(id);
        return mapToResponse(bed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedResponse> getBeds(Long roomId, BedStatus status) {
        List<Bed> beds;
        if (roomId != null && status != null) {
            beds = bedRepository.findByRoomIdAndStatus(roomId, status);
        } else if (roomId != null) {
            beds = bedRepository.findByRoomId(roomId);
        } else if (status != null) {
            beds = bedRepository.findByStatus(status);
        } else {
            beds = bedRepository.findAll();
        }

        return beds.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedResponse> getBedsByRoomId(Long roomId) {
        if (roomId == null) {
            throw new BadRequestException("Room ID must not be null.");
        }
        // Verify room exists
        roomService.getRoomEntity(roomId);
        return bedRepository.findByRoomId(roomId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public BedResponse updateBed(Long id, BedRequest request) {
        validateBedRequest(request);

        Bed existingBed = getBedEntity(id);
        Room targetRoom = roomService.getRoomEntity(request.getRoomId());
        String trimmedBedNumber = request.getBedNumber().trim();

        if (bedRepository.existsByRoomIdAndBedNumberIgnoreCaseAndIdNot(targetRoom.getId(), trimmedBedNumber, id)) {
            throw new DuplicateResourceException(
                    "Bed number '" + trimmedBedNumber + "' is already in use in Room " + targetRoom.getRoomNumber() + "."
            );
        }

        existingBed.setBedNumber(trimmedBedNumber);
        existingBed.setRoom(targetRoom);
        if (request.getStatus() != null && request.getStatus() != existingBed.getStatus()) {
            validateBedStatusChange(existingBed, request.getStatus());
            existingBed.setStatus(request.getStatus());
        }

        Bed updated = bedRepository.save(existingBed);
        return mapToResponse(updated);
    }

    @Override
    public BedResponse updateBedStatus(Long id, BedStatus status) {
        if (status == null) {
            throw new BadRequestException("Bed status must not be null.");
        }

        Bed existingBed = getBedEntity(id);

        if (status != existingBed.getStatus()) {
            validateBedStatusChange(existingBed, status);
            existingBed.setStatus(status);
        }

        Bed updated = bedRepository.save(existingBed);
        return mapToResponse(updated);
    }

    private void validateBedStatusChange(Bed bed, BedStatus newStatus) {
        if (newStatus != BedStatus.OCCUPIED) {
            patientRepository.findByAssignedBedIdAndStatus(bed.getId(), PatientStatus.ADMITTED)
                    .ifPresent(patient -> {
                        throw new BadRequestException(
                                "Cannot change status of Bed '" + bed.getBedNumber() + "' to " + newStatus +
                                        ": Patient " + patient.getName() + " (" + patient.getPatientCode() +
                                        ") is currently admitted to this bed. Discharge the patient first."
                        );
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Bed getBedEntity(Long id) {
        if (id == null) {
            throw new BadRequestException("Bed ID must not be null.");
        }
        return bedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found with ID: " + id));
    }

    private void validateBedRequest(BedRequest request) {
        if (request == null) {
            throw new BadRequestException("Bed request body cannot be null.");
        }
        if (request.getBedNumber() == null || request.getBedNumber().trim().isEmpty()) {
            throw new BadRequestException("Bed number is required.");
        }
        if (request.getRoomId() == null) {
            throw new BadRequestException("Associated room ID is required.");
        }
    }

    private BedResponse mapToResponse(Bed bed) {
        Room room = bed.getRoom();
        return new BedResponse(
                bed.getId(),
                bed.getBedNumber(),
                bed.getStatus(),
                room != null ? room.getId() : null,
                room != null ? room.getRoomNumber() : null,
                room != null ? room.getRoomType() : null,
                room != null ? room.getWardDepartment() : null
        );
    }
}

