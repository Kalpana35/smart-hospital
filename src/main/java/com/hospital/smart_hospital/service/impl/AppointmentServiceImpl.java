package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.AppointmentBookingRequest;
import com.hospital.smart_hospital.dto.AppointmentResponse;
import com.hospital.smart_hospital.dto.DoctorOptionDto;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Appointment;
import com.hospital.smart_hospital.model.AppointmentStatus;
import com.hospital.smart_hospital.model.DoctorApprovalStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.AppointmentRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AppointmentResponse bookAppointment(AppointmentBookingRequest request, Authentication authentication) {
        if (request == null) {
            throw new BadRequestException("Appointment request cannot be null.");
        }
        if (request.getDoctorId() == null) {
            throw new BadRequestException("Doctor selection is required.");
        }
        if (request.getAppointmentDateTime() == null) {
            throw new BadRequestException("Appointment date and time is required.");
        }
        if (request.getAppointmentDateTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BadRequestException("Appointment date and time must be in the future.");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("Reason for visit is required.");
        }

        // Determine patient
        Patient patient;
        boolean isPatient = hasRole(authentication, "ROLE_PATIENT");
        if (isPatient) {
            patient = patientRepository.findByUserUsernameIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new BadRequestException(
                            "No hospital patient record is linked to user account: " + authentication.getName()));
        } else {
            if (request.getPatientId() == null) {
                throw new BadRequestException("Patient ID is required for administrative/doctor booking.");
            }
            patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));
        }

        // Validate Doctor
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));
        if (doctor.getRole() != Role.ROLE_DOCTOR) {
            throw new BadRequestException("Selected user (" + doctor.getFullName() + ") is not a doctor.");
        }

        // Check for doctor schedule conflicts (within 30 minute window)
        LocalDateTime requestedTime = request.getAppointmentDateTime();
        boolean conflict = appointmentRepository.existsByDoctorIdAndStatusAndAppointmentDateTimeBetween(
                doctor.getId(),
                AppointmentStatus.SCHEDULED,
                requestedTime.minusMinutes(29),
                requestedTime.plusMinutes(29)
        );
        if (conflict) {
            throw new BadRequestException(
                    "Doctor " + doctor.getFullName() + " already has an active appointment scheduled around this time. Please select another time slot.");
        }

        String dept = request.getDepartment();
        if (dept == null || dept.trim().isEmpty()) {
            dept = "General Medicine";
        }

        Appointment appointment = new Appointment(
                patient,
                doctor,
                requestedTime,
                dept.trim(),
                request.getReason().trim()
        );

        Appointment saved = appointmentRepository.save(appointment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Unauthenticated");
        }

        if (hasRole(authentication, "ROLE_PATIENT")) {
            return appointmentRepository.findByPatientUserUsernameIgnoreCaseOrderByAppointmentDateTimeDesc(authentication.getName())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else if (hasRole(authentication, "ROLE_DOCTOR")) {
            return appointmentRepository.findByDoctorUsernameIgnoreCaseOrderByAppointmentDateTimeAsc(authentication.getName())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            // ADMIN
            return appointmentRepository.findAllByOrderByAppointmentDateTimeDesc()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateTimeAsc(doctorId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateTimeDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AppointmentResponse cancelAppointment(Long id, String reason, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // Enforce ownership: If patient, must be their appointment
        if (hasRole(authentication, "ROLE_PATIENT")) {
            if (appointment.getPatient().getUser() == null
                    || !appointment.getPatient().getUser().getUsername().equalsIgnoreCase(authentication.getName())) {
                throw new AccessDeniedException("You cannot cancel appointments of other patients.");
            }
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel an already completed appointment.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Appointment is already cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        String cancelNote = "Cancellation Reason: " + (reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by user");
        appointment.setNotes(appointment.getNotes() != null
                ? appointment.getNotes() + "\n" + cancelNote
                : cancelNote);

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    @Override
    public AppointmentResponse completeAppointment(Long id, String notes, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));

        // Only assigned Doctor or Admin can mark completed
        if (hasRole(authentication, "ROLE_DOCTOR")) {
            if (!appointment.getDoctor().getUsername().equalsIgnoreCase(authentication.getName())) {
                throw new AccessDeniedException("Only the assigned doctor or an administrator can complete this appointment.");
            }
        } else if (!hasRole(authentication, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Patients cannot mark appointments as completed.");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete a cancelled appointment.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        if (notes != null && !notes.trim().isEmpty()) {
            appointment.setNotes(appointment.getNotes() != null
                    ? appointment.getNotes() + "\nDoctor Consultation Note: " + notes.trim()
                    : "Doctor Consultation Note: " + notes.trim());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorOptionDto> getAvailableDoctors() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_DOCTOR && u.isEnabled() && u.getApprovalStatus() == DoctorApprovalStatus.APPROVED)
                .map(u -> new DoctorOptionDto(u.getId(), u.getUsername(), u.getFullName()))
                .toList();
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals(role));
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getPatient().getPatientCode(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                appointment.getAppointmentDateTime(),
                appointment.getDepartment(),
                appointment.getReason(),
                appointment.getNotes(),
                appointment.getStatus(),
                appointment.getCreatedAt()
        );
    }
}
