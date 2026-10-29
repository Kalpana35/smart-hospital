package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.DoctorApprovalStatus;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByMedicalRegistrationNumberIgnoreCase(String medicalRegistrationNumber);

    boolean existsByMedicalRegistrationNumberIgnoreCase(String medicalRegistrationNumber);

    List<User> findByRole(Role role);

    List<User> findByRoleAndApprovalStatus(Role role, DoctorApprovalStatus approvalStatus);
}

