package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.ConsultationRequest;
import com.hospital.smart_hospital.dto.ConsultationResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Appointment;
import com.hospital.smart_hospital.model.AppointmentStatus;
import com.hospital.smart_hospital.model.Consultation;
import com.hospital.smart_hospital.model.ConsultationStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.ConsultationRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.ConsultationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    public ConsultationServiceImpl(
            ConsultationRepository consultationRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.consultationRepository = consultationRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public ConsultationResponse recordConsultation(ConsultationRequest request, String doctorUsername) {
        if (request == null) {
            throw new BadRequestException("Consultation request cannot be null.");
        }
        if (request.getPatientId() == null) {
            throw new BadRequestException("Patient ID is required.");
        }
        if (request.getDiagnosis() == null || request.getDiagnosis().trim().isEmpty()) {
            throw new BadRequestException("Diagnosis is required.");
        }

        User doctor = userRepository.findByUsernameIgnoreCase(doctorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with username: " + doctorUsername));

        if (doctor.getRole() != Role.ROLE_DOCTOR && doctor.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("Only medical doctors can record patient consultations.");
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId()).orElse(null);
            if (appointment != null) {
                appointment.setStatus(AppointmentStatus.COMPLETED);
                if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                    appointment.setNotes(request.getNotes().trim());
                }
                appointmentRepository.save(appointment);
            }
        }

        // Also update latest clinical diagnosis on the patient record if provided
        if (request.getDiagnosis() != null && !request.getDiagnosis().trim().isEmpty()) {
            patient.setDiagnosis(request.getDiagnosis().trim());
            patientRepository.save(patient);
        }

        Consultation consultation = new Consultation(
                patient,
                doctor,
                appointment,
                LocalDateTime.now(),
                request.getDiagnosis().trim(),
                request.getTreatment() != null ? request.getTreatment().trim() : null,
                request.getPrescription() != null ? request.getPrescription().trim() : null,
                request.getNotes() != null ? request.getNotes().trim() : null,
                request.getStatus() != null ? request.getStatus() : ConsultationStatus.COMPLETED
        );

        Consultation saved = consultationRepository.save(consultation);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getConsultationsForPatient(Long patientId, String requestingUsername, boolean isStaff) {
        if (patientId == null) {
            throw new BadRequestException("Patient ID is required.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        if (!isStaff) {
            // Patient viewing their own records: check user link
            if (patient.getUser() == null || !patient.getUser().getUsername().equalsIgnoreCase(requestingUsername)) {
                throw new AccessDeniedException("Access Denied: You can only view your own medical consultation records.");
            }
        }

        return consultationRepository.findByPatientIdOrderByConsultationDateTimeDesc(patientId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getMyConsultations(String username, String roleName) {
        if ("ROLE_DOCTOR".equals(roleName)) {
            User doctor = userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with username: " + username));
            return consultationRepository.findByDoctorIdOrderByConsultationDateTimeDesc(doctor.getId()).stream()
                    .map(this::mapToResponse)
                    .toList();
        } else if ("ROLE_PATIENT".equals(roleName)) {
            return consultationRepository.findByPatientUserUsernameIgnoreCaseOrderByConsultationDateTimeDesc(username).stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            return getAllConsultations();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getAllConsultations() {
        return consultationRepository.findAllByOrderByConsultationDateTimeDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ConsultationResponse mapToResponse(Consultation c) {
        return new ConsultationResponse(
                c.getId(),
                c.getPatient() != null ? c.getPatient().getId() : null,
                c.getPatient() != null ? c.getPatient().getPatientCode() : "N/A",
                c.getPatient() != null ? c.getPatient().getName() : "N/A",
                c.getDoctor() != null ? c.getDoctor().getId() : null,
                c.getDoctor() != null ? c.getDoctor().getFullName() : "N/A",
                c.getAppointment() != null ? c.getAppointment().getId() : null,
                c.getConsultationDateTime(),
                c.getDiagnosis(),
                c.getTreatment(),
                c.getPrescription(),
                c.getNotes(),
                c.getStatus(),
                c.getCreatedAt()
        );
    }
}
