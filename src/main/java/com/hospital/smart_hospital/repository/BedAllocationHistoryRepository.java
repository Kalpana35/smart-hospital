package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.BedAllocationHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BedAllocationHistoryRepository extends JpaRepository<BedAllocationHistory, Long> {

    List<BedAllocationHistory> findAllByOrderByAllocatedAtDesc();

    Optional<BedAllocationHistory> findByPatientIdAndActiveTrue(Long patientId);

    List<BedAllocationHistory> findByPatientIdOrderByAllocatedAtDesc(Long patientId);

    List<BedAllocationHistory> findByBedIdOrderByAllocatedAtDesc(Long bedId);
}
