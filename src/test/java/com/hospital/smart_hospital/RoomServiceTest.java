package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.RoomRequest;
import com.hospital.smart_hospital.dto.RoomResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.RoomRepository;
import com.hospital.smart_hospital.service.impl.RoomServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BedRepository bedRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("R-101", "ICU", "Cardiology Ward");
        sampleRoom.setId(1L);
    }

    @Test
    @DisplayName("Should create room successfully when room number is unique")
    void testCreateRoom_Success() {
        RoomRequest request = new RoomRequest("R-101", "ICU", "Cardiology Ward");

        when(roomRepository.existsByRoomNumberIgnoreCase("R-101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);
        when(bedRepository.countByRoomId(1L)).thenReturn(0L);
        when(bedRepository.countByRoomIdAndStatus(1L, BedStatus.AVAILABLE)).thenReturn(0L);

        RoomResponse response = roomService.createRoom(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRoomNumber()).isEqualTo("R-101");
        assertThat(response.getRoomType()).isEqualTo("ICU");
        assertThat(response.getWardDepartment()).isEqualTo("Cardiology Ward");
        assertThat(response.getTotalBeds()).isZero();
        assertThat(response.getAvailableBeds()).isZero();

        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when room number already exists")
    void testCreateRoom_DuplicateRoomNumber() {
        RoomRequest request = new RoomRequest("R-101", "ICU", "Cardiology Ward");

        when(roomRepository.existsByRoomNumberIgnoreCase("R-101")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Room number 'R-101' already exists.");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when room number is blank")
    void testCreateRoom_BlankRoomNumber() {
        RoomRequest request = new RoomRequest("  ", "ICU", "Cardiology Ward");

        assertThatThrownBy(() -> roomService.createRoom(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Room number is required.");
    }

    @Test
    @DisplayName("Should get room by ID successfully")
    void testGetRoomById_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(bedRepository.countByRoomId(1L)).thenReturn(2L);
        when(bedRepository.countByRoomIdAndStatus(1L, BedStatus.AVAILABLE)).thenReturn(1L);

        RoomResponse response = roomService.getRoomById(1L);

        assertThat(response.getRoomNumber()).isEqualTo("R-101");
        assertThat(response.getTotalBeds()).isEqualTo(2);
        assertThat(response.getAvailableBeds()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when room ID does not exist")
    void testGetRoomById_NotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with ID: 999");
    }

    @Test
    @DisplayName("Should update room details and validate unique room number")
    void testUpdateRoom_Success() {
        RoomRequest updateRequest = new RoomRequest("R-102", "GENERAL", "Emergency Ward");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.existsByRoomNumberIgnoreCaseAndIdNot("R-102", 1L)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);

        RoomResponse response = roomService.updateRoom(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(sampleRoom.getRoomNumber()).isEqualTo("R-102");
        assertThat(sampleRoom.getRoomType()).isEqualTo("GENERAL");
        assertThat(sampleRoom.getWardDepartment()).isEqualTo("Emergency Ward");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating to an existing room number")
    void testUpdateRoom_DuplicateRoomNumber() {
        RoomRequest updateRequest = new RoomRequest("R-200", "GENERAL", "Emergency Ward");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.existsByRoomNumberIgnoreCaseAndIdNot("R-200", 1L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.updateRoom(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in use by another room");
    }
}

