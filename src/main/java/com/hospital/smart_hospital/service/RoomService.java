package com.hospital.smart_hospital.service;

import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.dto.RoomResponse;
import com.hospital.smart_hospital.model.Room;
import java.util.List;

public interface RoomService {

    RoomResponse createRoom(RoomRequest request);

    RoomResponse getRoomById(Long id);

    RoomResponse getRoomByNumber(String roomNumber);

    List<RoomResponse> getAllRooms();

    RoomResponse updateRoom(Long id, RoomRequest request);

    Room getRoomEntity(Long id);
}

