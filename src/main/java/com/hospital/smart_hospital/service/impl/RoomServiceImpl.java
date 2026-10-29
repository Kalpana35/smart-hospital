package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.dto.RoomResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.service.RoomService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    public RoomServiceImpl(RoomRepository roomRepository, BedRepository bedRepository) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
    }

    @Override
    public RoomResponse createRoom(RoomRequest request) {
        validateRoomRequest(request);

        String trimmedRoomNumber = request.getRoomNumber().trim();
        if (roomRepository.existsByRoomNumberIgnoreCase(trimmedRoomNumber)) {
            throw new DuplicateResourceException("Room number '" + trimmedRoomNumber + "' already exists.");
        }

        Room room = new Room(
                trimmedRoomNumber,
                request.getRoomType().trim(),
                request.getWardDepartment().trim()
        );

        Room saved = roomRepository.save(room);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = getRoomEntity(id);
        return mapToResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomByNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new BadRequestException("Room number cannot be empty.");
        }
        Room room = roomRepository.findByRoomNumberIgnoreCase(roomNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with room number: " + roomNumber.trim()));
        return mapToResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        validateRoomRequest(request);

        Room existingRoom = getRoomEntity(id);
        String trimmedRoomNumber = request.getRoomNumber().trim();

        if (roomRepository.existsByRoomNumberIgnoreCaseAndIdNot(trimmedRoomNumber, id)) {
            throw new DuplicateResourceException("Room number '" + trimmedRoomNumber + "' is already in use by another room.");
        }

        existingRoom.setRoomNumber(trimmedRoomNumber);
        existingRoom.setRoomType(request.getRoomType().trim());
        existingRoom.setWardDepartment(request.getWardDepartment().trim());

        Room updated = roomRepository.save(existingRoom);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Room getRoomEntity(Long id) {
        if (id == null) {
            throw new BadRequestException("Room ID must not be null.");
        }
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
    }

    private void validateRoomRequest(RoomRequest request) {
        if (request == null) {
            throw new BadRequestException("Room request body cannot be null.");
        }
        if (request.getRoomNumber() == null || request.getRoomNumber().trim().isEmpty()) {
            throw new BadRequestException("Room number is required.");
        }
        if (request.getRoomType() == null || request.getRoomType().trim().isEmpty()) {
            throw new BadRequestException("Room type is required.");
        }
        if (request.getWardDepartment() == null || request.getWardDepartment().trim().isEmpty()) {
            throw new BadRequestException("Ward/department name is required.");
        }
    }

    private RoomResponse mapToResponse(Room room) {
        int totalBeds = (int) bedRepository.countByRoomId(room.getId());
        int availableBeds = (int) bedRepository.countByRoomIdAndStatus(room.getId(), BedStatus.AVAILABLE);

        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getWardDepartment(),
                totalBeds,
                availableBeds
        );
    }
}

