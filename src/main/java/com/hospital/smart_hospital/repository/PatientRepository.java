package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.PatientStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientCodeIgnoreCase(String patientCode);

    boolean existsByPatientCodeIgnoreCase(String patientCode);

    List<Patient> findByStatus(PatientStatus status);

    Optional<Patient> findByAssignedBedIdAndStatus(Long bedId, PatientStatus status);

    boolean existsByAssignedBedIdAndStatus(Long bedId, PatientStatus status);

    Optional<Patient> findByUserId(Long userId);

    Optional<Patient> findByUserUsernameIgnoreCase(String username);

    boolean existsByUserId(Long userId);
}

