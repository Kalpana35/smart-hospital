package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Consultation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByPatientIdOrderByConsultationDateTimeDesc(Long patientId);

    List<Consultation> findByDoctorIdOrderByConsultationDateTimeDesc(Long doctorId);

    List<Consultation> findAllByOrderByConsultationDateTimeDesc();

    List<Consultation> findByPatientUserUsernameIgnoreCaseOrderByConsultationDateTimeDesc(String username);
}
