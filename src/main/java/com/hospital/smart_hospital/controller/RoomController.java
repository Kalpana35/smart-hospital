package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.BedResponse;
import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.dto.RoomResponse;
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.RoomService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final BedService bedService;

    public RoomController(RoomService roomService, BedService bedService) {
        this.roomService = roomService;
        this.bedService = bedService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@RequestBody RoomRequest request) {
        RoomResponse created = roomService.createRoom(request);
        return new ResponseEntity<>(
                ApiResponse.ok("Room created successfully", created),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.ok("Rooms retrieved successfully", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.ok("Room retrieved successfully", room));
    }

    @GetMapping("/by-number/{roomNumber}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomByNumber(@PathVariable String roomNumber) {
        RoomResponse room = roomService.getRoomByNumber(roomNumber);
        return ResponseEntity.ok(ApiResponse.ok("Room retrieved successfully", room));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @RequestBody RoomRequest request
    ) {
        RoomResponse updated = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Room updated successfully", updated));
    }

    @GetMapping("/{id}/beds")
    public ResponseEntity<ApiResponse<List<BedResponse>>> getBedsForRoom(@PathVariable Long id) {
        List<BedResponse> beds = bedService.getBedsByRoomId(id);
        return ResponseEntity.ok(ApiResponse.ok("Room beds retrieved successfully", beds));
    }
}

