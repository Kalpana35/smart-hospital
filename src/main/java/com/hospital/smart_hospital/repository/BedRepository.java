package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByRoomId(Long roomId);

    List<Bed> findByStatus(BedStatus status);

    List<Bed> findByRoomIdAndStatus(Long roomId, BedStatus status);

    boolean existsByRoomIdAndBedNumberIgnoreCase(Long roomId, String bedNumber);

    boolean existsByRoomIdAndBedNumberIgnoreCaseAndIdNot(Long roomId, String bedNumber, Long id);

    Optional<Bed> findByRoomIdAndBedNumberIgnoreCase(Long roomId, String bedNumber);

    long countByRoomId(Long roomId);

    long countByRoomIdAndStatus(Long roomId, BedStatus status);
}

