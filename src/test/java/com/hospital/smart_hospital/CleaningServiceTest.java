package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.CleaningStatisticsResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedCleaningLog;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.repository.BedCleaningLogRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.service.impl.CleaningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleaningServiceTest {

    @Mock
    private BedRepository bedRepository;

    @Mock
    private BedCleaningLogRepository bedCleaningLogRepository;

    @InjectMocks
    private CleaningServiceImpl cleaningService;

    private Room sampleRoom;
    private Bed sampleBed;
    private BedCleaningLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("R-101", "ICU", "ICU");
        sampleRoom.setId(1L);

        sampleBed = new Bed("BED-1", sampleRoom, BedStatus.DIRTY);
        sampleBed.setId(10L);
        sampleBed.setCleaningPriority(1);
        sampleBed.setDirtySince(LocalDateTime.now().minusMinutes(25));

        sampleLog = new BedCleaningLog(10L, "BED-1", 1L, "R-101", "ICU", 1);
        sampleLog.setId(100L);
    }

    @Test
    @DisplayName("Should successfully assign cleaner to a DIRTY bed")
    void testAssignCleaner_Success() {
        when(bedRepository.findById(10L)).thenReturn(Optional.of(sampleBed));
        when(bedCleaningLogRepository.findTopByBedIdAndStatusInOrderByDirtyAtDesc(10L, List.of("DIRTY", "IN_PROGRESS")))
                .thenReturn(Optional.of(sampleLog));
        when(bedRepository.save(any(Bed.class))).thenReturn(sampleBed);
        when(bedCleaningLogRepository.save(any(BedCleaningLog.class))).thenReturn(sampleLog);

        BedCleaningLog result = cleaningService.assignCleaner(10L, "Cleaner Sarah");

        assertThat(result).isNotNull();
        assertThat(sampleBed.getCleaningAssignedTo()).isEqualTo("Cleaner Sarah");
        assertThat(sampleBed.getCleaningAssignedAt()).isNotNull();
        assertThat(sampleLog.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(sampleLog.getAssignedTo()).isEqualTo("Cleaner Sarah");

        verify(bedRepository).save(sampleBed);
        verify(bedCleaningLogRepository).save(sampleLog);
    }

    @Test
    @DisplayName("Should reject cleaner assignment if bed is not DIRTY")
    void testAssignCleaner_NotDirty_ThrowsBadRequest() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        when(bedRepository.findById(10L)).thenReturn(Optional.of(sampleBed));

        assertThatThrownBy(() -> cleaningService.assignCleaner(10L, "Cleaner Sarah"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not DIRTY");

        verify(bedRepository, never()).save(any());
        verify(bedCleaningLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject cleaner assignment when cleaner name is empty")
    void testAssignCleaner_EmptyName_ThrowsBadRequest() {
        assertThatThrownBy(() -> cleaningService.assignCleaner(10L, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cleaner name/staff ID is required");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bed does not exist for cleaner assignment")
    void testAssignCleaner_BedNotFound() {
        when(bedRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cleaningService.assignCleaner(999L, "Cleaner Sarah"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bed not found with ID: 999");
    }

    @Test
    @DisplayName("Should successfully complete cleaning and transition bed to AVAILABLE")
    void testCompleteCleaning_Success() {
        sampleBed.setStatus(BedStatus.DIRTY);
        sampleBed.setCleaningAssignedTo("Cleaner Sarah");
        sampleLog.setStatus("IN_PROGRESS");
        sampleLog.setDirtyAt(LocalDateTime.now().minusMinutes(30));

        when(bedRepository.findById(10L)).thenReturn(Optional.of(sampleBed));
        when(bedCleaningLogRepository.findTopByBedIdAndStatusInOrderByDirtyAtDesc(10L, List.of("DIRTY", "IN_PROGRESS")))
                .thenReturn(Optional.of(sampleLog));
        when(bedRepository.save(any(Bed.class))).thenReturn(sampleBed);
        when(bedCleaningLogRepository.save(any(BedCleaningLog.class))).thenReturn(sampleLog);

        Bed resultBed = cleaningService.completeCleaning(10L, "Cleaner Sarah", "Sanitized with hospital-grade disinfectant");

        assertThat(resultBed.getStatus()).isEqualTo(BedStatus.AVAILABLE);
        assertThat(resultBed.getCleaningAssignedTo()).isNull();
        assertThat(resultBed.getCleaningAssignedAt()).isNull();
        assertThat(resultBed.getCleaningPriority()).isNull();
        assertThat(sampleLog.getStatus()).isEqualTo("COMPLETED");
        assertThat(sampleLog.getCleanedAt()).isNotNull();
        assertThat(sampleLog.getDurationMinutes()).isNotNull();

        verify(bedRepository).save(sampleBed);
        verify(bedCleaningLogRepository).save(sampleLog);
    }

    @Test
    @DisplayName("Should reject cleaning completion if bed is not DIRTY")
    void testCompleteCleaning_NotDirty_ThrowsBadRequest() {
        sampleBed.setStatus(BedStatus.AVAILABLE);
        when(bedRepository.findById(10L)).thenReturn(Optional.of(sampleBed));

        assertThatThrownBy(() -> cleaningService.completeCleaning(10L, "Cleaner Sarah", "Notes"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not in DIRTY status");

        verify(bedRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve cleaning queue ordered by priority and dirty timestamp")
    void testGetCleaningQueue() {
        when(bedCleaningLogRepository.findByStatusInOrderByPriorityAscDirtyAtAsc(List.of("DIRTY", "IN_PROGRESS")))
                .thenReturn(List.of(sampleLog));

        List<BedCleaningLog> queue = cleaningService.getCleaningQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getBedNumber()).isEqualTo("BED-1");
        assertThat(queue.get(0).getPriority()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return accurate cleaning turnaround statistics")
    void testGetCleaningStatistics() {
        sampleLog.setStatus("COMPLETED");
        sampleLog.setDurationMinutes(24L);
        sampleLog.setCleanedAt(LocalDateTime.now().minusHours(1));

        when(bedCleaningLogRepository.findTop50ByStatusOrderByCleanedAtDesc("COMPLETED")).thenReturn(List.of(sampleLog));
        when(bedCleaningLogRepository.findByStatusInOrderByPriorityAscDirtyAtAsc(List.of("DIRTY", "IN_PROGRESS")))
                .thenReturn(List.of());

        CleaningStatisticsResponse stats = cleaningService.getCleaningStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.getCompletedToday()).isEqualTo(1);
        assertThat(stats.getTotalDirtyBeds()).isEqualTo(0);
        assertThat(stats.getAverageTurnaroundMinutes()).isEqualTo(24.0);
    }
}
