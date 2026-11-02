package com.hospital.smart_hospital.service.impl;

import com.hospital.smart_hospital.dto.PatientAdmissionRequest;
import com.hospital.smart_hospital.dto.PatientResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.AllocationType;
import com.hospital.smart_hospital.model.Bed;
import com.hospital.smart_hospital.model.BedStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.PatientStatus;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.Room;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.model.BedCleaningLog;
import com.hospital.smart_hospital.repository.BedCleaningLogRepository;
import com.hospital.smart_hospital.repository.BedRepository;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.service.BedAllocationHistoryService;
import com.hospital.smart_hospital.service.BedService;
import com.hospital.smart_hospital.service.PatientService;
import com.hospital.smart_hospital.util.PasswordValidator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final BedRepository bedRepository;
    private final BedService bedService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BedAllocationHistoryService bedAllocationHistoryService;
    private final BedCleaningLogRepository bedCleaningLogRepository;

    public PatientServiceImpl(
            PatientRepository patientRepository,
            BedRepository bedRepository,
            BedService bedService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BedAllocationHistoryService bedAllocationHistoryService,
            BedCleaningLogRepository bedCleaningLogRepository
    ) {
        this.patientRepository = patientRepository;
        this.bedRepository = bedRepository;
        this.bedService = bedService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bedAllocationHistoryService = bedAllocationHistoryService;
        this.bedCleaningLogRepository = bedCleaningLogRepository;
    }

    @Override
    public PatientResponse admitPatient(PatientAdmissionRequest request) {
        validateAdmissionRequest(request);

        String trimmedCode = request.getPatientCode().trim();
        if (patientRepository.existsByPatientCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateResourceException("Patient with code '" + trimmedCode + "' already exists.");
        }

        Bed bed = bedService.getBedEntity(request.getBedId());

        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw new BadRequestException("Cannot admit patient: Bed '" + bed.getBedNumber() + "' is already OCCUPIED.");
        }
        if (bed.getStatus() == BedStatus.MAINTENANCE) {
            throw new BadRequestException("Cannot admit patient: Bed '" + bed.getBedNumber() + "' is currently under MAINTENANCE.");
        }
        if (bed.getStatus() == BedStatus.DIRTY) {
            throw new BadRequestException("Cannot admit patient: Bed '" + bed.getBedNumber() + "' is DIRTY and requires cleaning before admission.");
        }
        if (bed.getStatus() != BedStatus.AVAILABLE) {
            throw new BadRequestException("Cannot admit patient: Bed '" + bed.getBedNumber() + "' is not AVAILABLE. Current status: " + bed.getStatus());
        }

        if (patientRepository.existsByAssignedBedIdAndStatus(bed.getId(), PatientStatus.ADMITTED)) {
            throw new BadRequestException("Cannot admit patient: An active patient is already assigned to Bed '" + bed.getBedNumber() + "'.");
        }

        // Optional creation of linked portal account for emergency intake
        User portalUser = null;
        if (Boolean.TRUE.equals(request.getCreatePortalAccount())) {
            String username = request.getPortalUsername();
            if (username == null || username.trim().isEmpty()) {
                username = trimmedCode.toLowerCase();
            }
            username = username.trim();
            if (userRepository.existsByUsernameIgnoreCase(username)) {
                throw new DuplicateResourceException("Username '" + username + "' is already taken for patient portal access.");
            }
            String password = request.getPortalPassword();
            PasswordValidator.validatePassword(password);
            portalUser = new User(
                    username,
                    passwordEncoder.encode(password.trim()),
                    request.getName().trim(),
                    Role.ROLE_PATIENT
            );
            portalUser = userRepository.save(portalUser);
        }

        // Validate manual override reason
        if (request.getAllocationType() == AllocationType.MANUAL_OVERRIDE) {
            if (request.getOverrideReason() == null || request.getOverrideReason().trim().isEmpty()) {
                throw new BadRequestException("Override reason is required when performing manual bed allocation.");
            }
        }

        // Mark bed as OCCUPIED
        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);

        // Create new patient record
        Patient patient = new Patient(
                trimmedCode,
                request.getName().trim(),
                request.getAge(),
                request.getGender().trim(),
                request.getDiagnosis() != null ? request.getDiagnosis().trim() : null,
                bed
        );
        if (request.getExpectedDischargeDate() != null) {
            patient.setExpectedDischargeDate(request.getExpectedDischargeDate());
        } else {
            patient.setExpectedDischargeDate(LocalDateTime.now().plusDays(3));
        }
        if (portalUser != null) {
            patient.setUser(portalUser);
        }

        Patient saved = patientRepository.save(patient);

        // Record bed allocation history
        String allocatedBy = "System";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            allocatedBy = auth.getName();
        }
        bedAllocationHistoryService.recordAllocation(
                bed,
                saved,
                allocatedBy,
                request.getAllocationType() != null ? request.getAllocationType() : AllocationType.RECOMMENDED,
                request.getOverrideReason()
        );

        return mapToResponse(saved);
    }

    @Override
    public PatientResponse dischargePatient(Long patientId) {
        if (patientId == null) {
            throw new BadRequestException("Patient ID must not be null.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        if (patient.getStatus() == PatientStatus.DISCHARGED) {
            throw new BadRequestException("Patient with ID " + patientId + " is already discharged.");
        }

        // Release the bed to DIRTY status for cleaning inspection
        Bed bed = patient.getAssignedBed();
        if (bed != null) {
            bed.setStatus(BedStatus.DIRTY);
            bed.setDirtySince(LocalDateTime.now());

            int priority = 3;
            String dept = bed.getRoom() != null && bed.getRoom().getWardDepartment() != null
                    ? bed.getRoom().getWardDepartment().toUpperCase() : "";
            if (dept.contains("ICU") || dept.contains("EMERG")) {
                priority = 1;
            } else if (dept.contains("SURG") || dept.contains("ISO")) {
                priority = 2;
            }
            bed.setCleaningPriority(priority);
            bedRepository.save(bed);

            BedCleaningLog cleaningLog = new BedCleaningLog(
                    bed.getId(),
                    bed.getBedNumber(),
                    bed.getRoom() != null ? bed.getRoom().getId() : null,
                    bed.getRoom() != null ? bed.getRoom().getRoomNumber() : null,
                    bed.getRoom() != null ? bed.getRoom().getWardDepartment() : null,
                    priority
            );
            bedCleaningLogRepository.save(cleaningLog);
        }

        patient.setStatus(PatientStatus.DISCHARGED);
        patient.setDischargeDate(LocalDateTime.now());

        Patient updated = patientRepository.save(patient);
        bedAllocationHistoryService.releaseAllocation(patientId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        if (id == null) {
            throw new BadRequestException("Patient ID must not be null.");
        }
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
        return mapToResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientByCode(String patientCode) {
        if (patientCode == null || patientCode.trim().isEmpty()) {
            throw new BadRequestException("Patient code must not be empty.");
        }
        Patient patient = patientRepository.findByPatientCodeIgnoreCase(patientCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with code: " + patientCode.trim()));
        return mapToResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getPatients(PatientStatus status) {
        List<Patient> list = (status != null)
                ? patientRepository.findByStatus(status)
                : patientRepository.findAll();

        return list.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getMyPatientRecord(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Username must not be empty.");
        }
        Patient patient = patientRepository.findByUserUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No hospital patient record is linked to user account: " + username));
        return mapToResponse(patient);
    }

    @Override
    public PatientResponse createPatientPortalAccount(Long patientId, String username, String password) {
        if (patientId == null) {
            throw new BadRequestException("Patient ID is required.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        PasswordValidator.validatePassword(password);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
        if (patient.getUser() != null) {
            throw new BadRequestException("Patient already has a linked portal account: " + patient.getUser().getUsername());
        }
        String trimmedUsername = username.trim();
        if (userRepository.existsByUsernameIgnoreCase(trimmedUsername)) {
            throw new DuplicateResourceException("Username '" + trimmedUsername + "' is already taken.");
        }
        User user = new User(
                trimmedUsername,
                passwordEncoder.encode(password.trim()),
                patient.getName(),
                Role.ROLE_PATIENT
        );
        User savedUser = userRepository.save(user);
        patient.setUser(savedUser);
        Patient updated = patientRepository.save(patient);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getPredictiveDischarges() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(48);
        List<Patient> list = patientRepository.findByStatusAndExpectedDischargeDateIsNotNull(PatientStatus.ADMITTED);
        return list.stream()
                .filter(p -> p.getExpectedDischargeDate() != null && !p.getExpectedDischargeDate().isAfter(threshold))
                .sorted(Comparator.comparing(Patient::getExpectedDischargeDate))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PatientResponse updateExpectedDischargeDate(Long patientId, LocalDateTime expectedDate) {
        if (patientId == null) {
            throw new BadRequestException("Patient ID is required.");
        }
        if (expectedDate == null) {
            throw new BadRequestException("Expected discharge date is required.");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
        if (patient.getStatus() != PatientStatus.ADMITTED) {
            throw new BadRequestException("Cannot update expected discharge date for a discharged patient.");
        }
        patient.setExpectedDischargeDate(expectedDate);
        Patient updated = patientRepository.save(patient);
        return mapToResponse(updated);
    }

    private void validateAdmissionRequest(PatientAdmissionRequest request) {
        if (request == null) {
            throw new BadRequestException("Admission request body cannot be null.");
        }
        if (request.getPatientCode() == null || request.getPatientCode().trim().isEmpty()) {
            throw new BadRequestException("Patient code is required.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Patient name is required.");
        }
        if (request.getAge() == null || request.getAge() <= 0) {
            throw new BadRequestException("Valid patient age is required.");
        }
        if (request.getGender() == null || request.getGender().trim().isEmpty()) {
            throw new BadRequestException("Patient gender is required.");
        }
        if (request.getBedId() == null) {
            throw new BadRequestException("Selected bed ID is required.");
        }
    }

    private PatientResponse mapToResponse(Patient patient) {
        Bed bed = patient.getAssignedBed();
        Long bedId = null;
        String bedNumber = null;
        Long roomId = null;
        String roomNumber = null;
        String roomType = null;
        String wardDepartment = null;

        Double dailyRate = null;
        if (bed != null) {
            bedId = bed.getId();
            bedNumber = bed.getBedNumber();
            dailyRate = bed.getDailyCharge();
            Room room = bed.getRoom();
            if (room != null) {
                roomId = room.getId();
                roomNumber = room.getRoomNumber();
                roomType = room.getRoomType();
                wardDepartment = room.getWardDepartment();
            }
        }

        Long userId = null;
        String username = null;
        if (patient.getUser() != null) {
            userId = patient.getUser().getId();
            username = patient.getUser().getUsername();
        }

        PatientResponse resp = new PatientResponse(
                patient.getId(),
                patient.getPatientCode(),
                patient.getName(),
                patient.getAge(),
                patient.getGender(),
                patient.getDiagnosis(),
                patient.getStatus(),
                patient.getAdmissionDate(),
                patient.getDischargeDate(),
                bedId,
                bedNumber,
                roomId,
                roomNumber,
                roomType,
                wardDepartment,
                userId,
                username
        );
        resp.setExpectedDischargeDate(patient.getExpectedDischargeDate());
        resp.setDailyRate(dailyRate);
        return resp;
    }
}

