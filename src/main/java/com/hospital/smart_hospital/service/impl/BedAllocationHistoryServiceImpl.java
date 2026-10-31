package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.BedAllocationHistoryResponse;
import com.hospital.smart_hospital.model.AllocationType;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedAllocationHistory;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.repository.BedAllocationHistoryRepository;
import com.hospital.smart_hospital.service.BedAllocationHistoryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BedAllocationHistoryServiceImpl implements BedAllocationHistoryService {

    private final BedAllocationHistoryRepository historyRepository;

    public BedAllocationHistoryServiceImpl(BedAllocationHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedAllocationHistoryResponse> getAllAllocationHistory() {
        return historyRepository.findAllByOrderByAllocatedAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BedAllocationHistoryResponse> getAllocationHistoryByPatientId(Long patientId) {
        return historyRepository.findByPatientIdOrderByAllocatedAtDesc(patientId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public BedAllocationHistory recordAllocation(
            Bed bed,
            Patient patient,
            String allocatedBy,
            AllocationType type,
            String overrideReason
    ) {
        String actor = (allocatedBy != null && !allocatedBy.trim().isEmpty()) ? allocatedBy.trim() : "System";
        String roomNumber = (bed.getRoom() != null) ? bed.getRoom().getRoomNumber() : "Unknown";
        Long roomId = (bed.getRoom() != null) ? bed.getRoom().getId() : 0L;
        String ward = (bed.getRoom() != null) ? bed.getRoom().getWardDepartment() : "General";

        BedAllocationHistory history = new BedAllocationHistory(
                bed.getId(),
                bed.getBedNumber(),
                roomId,
                roomNumber,
                ward,
                patient.getId(),
                patient.getPatientCode(),
                patient.getName(),
                actor,
                type != null ? type : AllocationType.RECOMMENDED,
                overrideReason
        );

        return historyRepository.save(history);
    }

    @Override
    public void releaseAllocation(Long patientId) {
        if (patientId == null) {
            return;
        }
        Optional<BedAllocationHistory> activeOpt = historyRepository.findByPatientIdAndActiveTrue(patientId);
        if (activeOpt.isPresent()) {
            BedAllocationHistory active = activeOpt.get();
            active.setActive(false);
            active.setReleasedAt(LocalDateTime.now());
            historyRepository.save(active);
        }
    }

    private BedAllocationHistoryResponse mapToResponse(BedAllocationHistory entity) {
        return new BedAllocationHistoryResponse(
                entity.getId(),
                entity.getBedId(),
                entity.getBedNumber(),
                entity.getRoomId(),
                entity.getRoomNumber(),
                entity.getWardDepartment(),
                entity.getPatientId(),
                entity.getPatientCode(),
                entity.getPatientName(),
                entity.getAllocatedBy(),
                entity.getAllocationType(),
                entity.getOverrideReason(),
                entity.getAllocatedAt(),
                entity.getReleasedAt(),
                entity.isActive()
        );
    }
}
