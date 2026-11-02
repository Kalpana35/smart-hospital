package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.CleaningStatisticsResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedCleaningLog;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.repository.BedCleaningLogRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.service.CleaningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CleaningServiceImpl implements CleaningService {

    private final BedRepository bedRepository;
    private final BedCleaningLogRepository bedCleaningLogRepository;

    public CleaningServiceImpl(BedRepository bedRepository, BedCleaningLogRepository bedCleaningLogRepository) {
        this.bedRepository = bedRepository;
        this.bedCleaningLogRepository = bedCleaningLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedCleaningLog> getCleaningQueue() {
        return bedCleaningLogRepository.findByStatusInOrderByPriorityAscDirtyAtAsc(List.of("DIRTY", "IN_PROGRESS"));
    }

    @Override
    public BedCleaningLog assignCleaner(Long bedId, String cleanerName) {
        if (bedId == null) {
            throw new BadRequestException("Bed ID is required.");
        }
        if (cleanerName == null || cleanerName.trim().isEmpty()) {
            throw new BadRequestException("Cleaner name/staff ID is required.");
        }

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found with ID: " + bedId));

        if (bed.getStatus() != BedStatus.DIRTY) {
            throw new BadRequestException("Cannot assign cleaner: Bed '" + bed.getBedNumber() + "' is not DIRTY (Current status: " + bed.getStatus() + ").");
        }

        bed.setCleaningAssignedTo(cleanerName.trim());
        bed.setCleaningAssignedAt(LocalDateTime.now());
        bedRepository.save(bed);

        BedCleaningLog log = bedCleaningLogRepository.findTopByBedIdAndStatusInOrderByDirtyAtDesc(bedId, List.of("DIRTY", "IN_PROGRESS"))
                .orElseGet(() -> {
                    BedCleaningLog newLog = new BedCleaningLog(
                            bed.getId(),
                            bed.getBedNumber(),
                            bed.getRoom() != null ? bed.getRoom().getId() : null,
                            bed.getRoom() != null ? bed.getRoom().getRoomNumber() : null,
                            bed.getRoom() != null ? bed.getRoom().getWardDepartment() : null,
                            bed.getCleaningPriority() != null ? bed.getCleaningPriority() : 3
                    );
                    return bedCleaningLogRepository.save(newLog);
                });

        log.setAssignedTo(cleanerName.trim());
        log.setAssignedAt(LocalDateTime.now());
        log.setStatus("IN_PROGRESS");

        return bedCleaningLogRepository.save(log);
    }

    @Override
    public Bed completeCleaning(Long bedId, String cleanerName, String inspectionNotes) {
        if (bedId == null) {
            throw new BadRequestException("Bed ID is required.");
        }

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found with ID: " + bedId));

        if (bed.getStatus() != BedStatus.DIRTY) {
            throw new BadRequestException("Cannot complete cleaning: Bed '" + bed.getBedNumber() + "' is not in DIRTY status (Current: " + bed.getStatus() + ").");
        }

        LocalDateTime completedAt = LocalDateTime.now();

        // Release bed back to AVAILABLE
        bed.setStatus(BedStatus.AVAILABLE);
        bed.setCleaningAssignedTo(null);
        bed.setCleaningAssignedAt(null);
        bed.setCleaningPriority(null);
        bed.setDirtySince(null);
        Bed savedBed = bedRepository.save(bed);

        // Update cleaning log
        BedCleaningLog log = bedCleaningLogRepository.findTopByBedIdAndStatusInOrderByDirtyAtDesc(bedId, List.of("DIRTY", "IN_PROGRESS"))
                .orElseGet(() -> new BedCleaningLog(
                        bed.getId(),
                        bed.getBedNumber(),
                        bed.getRoom() != null ? bed.getRoom().getId() : null,
                        bed.getRoom() != null ? bed.getRoom().getRoomNumber() : null,
                        bed.getRoom() != null ? bed.getRoom().getWardDepartment() : null,
                        3
                ));

        if (cleanerName != null && !cleanerName.trim().isEmpty()) {
            log.setAssignedTo(cleanerName.trim());
        }
        log.setCleanedAt(completedAt);
        log.setStatus("COMPLETED");
        if (log.getDirtyAt() != null) {
            long minutes = Math.max(1, Duration.between(log.getDirtyAt(), completedAt).toMinutes());
            log.setDurationMinutes(minutes);
        } else {
            log.setDurationMinutes(20L); // Default estimate
        }
        if (inspectionNotes != null && !inspectionNotes.trim().isEmpty()) {
            log.setNotes(inspectionNotes.trim());
        } else {
            log.setNotes("Sanitized and disinfected. Confirmed ready for patient admission.");
        }
        bedCleaningLogRepository.save(log);

        return savedBed;
    }

    @Override
    @Transactional(readOnly = true)
    public CleaningStatisticsResponse getCleaningStatistics() {
        List<BedCleaningLog> queue = bedCleaningLogRepository.findByStatusInOrderByPriorityAscDirtyAtAsc(List.of("DIRTY", "IN_PROGRESS"));
        int dirtyCount = (int) queue.stream().filter(l -> "DIRTY".equals(l.getStatus())).count();
        int inProgressCount = (int) queue.stream().filter(l -> "IN_PROGRESS".equals(l.getStatus())).count();

        List<BedCleaningLog> completed = bedCleaningLogRepository.findTop50ByStatusOrderByCleanedAtDesc("COMPLETED");

        LocalDate today = LocalDate.now();
        int completedToday = (int) completed.stream()
                .filter(l -> l.getCleanedAt() != null && l.getCleanedAt().toLocalDate().isEqual(today))
                .count();

        double avgMinutes = completed.stream()
                .filter(l -> l.getDurationMinutes() != null && l.getDurationMinutes() > 0)
                .mapToLong(BedCleaningLog::getDurationMinutes)
                .average()
                .orElse(25.0);

        double roundedAvg = Math.round(avgMinutes * 10.0) / 10.0;

        return new CleaningStatisticsResponse(
                dirtyCount,
                inProgressCount,
                completedToday,
                roundedAvg,
                queue,
                completed
        );
    }
}
