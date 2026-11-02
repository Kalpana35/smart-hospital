package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.BedCleaningLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BedCleaningLogRepository extends JpaRepository<BedCleaningLog, Long> {

    List<BedCleaningLog> findByStatusInOrderByPriorityAscDirtyAtAsc(List<String> statuses);

    Optional<BedCleaningLog> findTopByBedIdAndStatusInOrderByDirtyAtDesc(Long bedId, List<String> statuses);

    List<BedCleaningLog> findByStatus(String status);

    List<BedCleaningLog> findTop50ByStatusOrderByCleanedAtDesc(String status);
}
