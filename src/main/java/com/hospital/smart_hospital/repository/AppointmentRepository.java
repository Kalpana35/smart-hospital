package com.hospital.smart_hospital.repository;

import com.hospital.smart_hospital.model.Appointment;
import com.hospital.smart_hospital.model.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId);

    List<Appointment> findByPatientUserUsernameIgnoreCaseOrderByAppointmentDateTimeDesc(String username);

    List<Appointment> findByDoctorIdOrderByAppointmentDateTimeAsc(Long doctorId);

    List<Appointment> findByDoctorUsernameIgnoreCaseOrderByAppointmentDateTimeAsc(String username);

    List<Appointment> findAllByOrderByAppointmentDateTimeDesc();

    boolean existsByDoctorIdAndStatusAndAppointmentDateTimeBetween(
            Long doctorId,
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
