package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.ChangePasswordRequest;
import com.hospital.smart_hospital.dto.DoctorRegistrationRequest;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.PatientSelfRegistrationRequest;
import com.hospital.smart_hospital.dto.RegisterRequest;
import com.hospital.smart_hospital.dto.StaffAccountRequest;
import com.hospital.smart_hospital.dto.UserResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.DoctorApprovalStatus;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.util.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Username and password are required.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
            );

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            // Establish standard HTTP session for cookie persistence
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            User user = userRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("User record not found"));

            return ResponseEntity.ok(ApiResponse.ok("Login successful", mapToResponse(user)));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        } catch (DisabledException ex) {
            Optional<User> optionalUser = userRepository.findByUsernameIgnoreCase(request.getUsername().trim());
            if (optionalUser.isPresent()) {
                User u = optionalUser.get();
                if (u.getApprovalStatus() == DoctorApprovalStatus.PENDING) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("Your doctor account registration is pending administrator approval. Please wait for an administrator to review and approve your credentials."));
                } else if (u.getApprovalStatus() == DoctorApprovalStatus.REJECTED) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("Your doctor account registration has been rejected. Please contact hospital administration for further details."));
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User account is disabled."));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthenticated"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return ResponseEntity.ok(ApiResponse.ok("Current user profile retrieved", mapToResponse(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @RequestBody RegisterRequest request,
            Authentication authentication
    ) {
        // Enforce that only authenticated ADMIN users can register new accounts
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only ADMIN can register new accounts."));
        }

        validateRegisterRequest(request);

        String trimmedUsername = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(trimmedUsername)) {
            throw new DuplicateResourceException("Username '" + trimmedUsername + "' already exists.");
        }

        // Validate role selection
        Role role = request.getRole();
        if (role == null) {
            throw new BadRequestException("User role is required (ROLE_DOCTOR or ROLE_PATIENT).");
        }

        User newUser = new User(
                trimmedUsername,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                role
        );

        User saved = userRepository.save(newUser);
        return new ResponseEntity<>(ApiResponse.ok("User registered successfully", mapToResponse(saved)), HttpStatus.CREATED);
    }

    @PostMapping("/register-patient")
    public ResponseEntity<ApiResponse<UserResponse>> registerPatient(
            @RequestBody PatientSelfRegistrationRequest request
    ) {
        validatePatientRegistrationRequest(request);

        String trimmedUsername = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(trimmedUsername)) {
            throw new DuplicateResourceException("Username '" + trimmedUsername + "' already exists.");
        }

        String trimmedCode = request.getPatientCode().trim();
        Patient patient = patientRepository.findByPatientCodeIgnoreCase(trimmedCode)
                .orElseThrow(() -> new BadRequestException(
                        "No hospital record found with Patient Code '" + trimmedCode + "'. Please verify your code or contact the hospital."));

        if (patient.getUser() != null) {
            throw new BadRequestException("A portal account is already linked to Patient Code '" + trimmedCode + "'. Please sign in or contact administration.");
        }

        // Verify that registering name matches hospital patient record
        String inputName = request.getFullName().trim().toLowerCase();
        String recordName = patient.getName().trim().toLowerCase();
        if (!inputName.equals(recordName)) {
            throw new BadRequestException("Full name does not match hospital records for Patient Code '" + trimmedCode + "'.");
        }

        User newUser = new User(
                trimmedUsername,
                passwordEncoder.encode(request.getPassword()),
                patient.getName(),
                Role.ROLE_PATIENT
        );

        User savedUser = userRepository.save(newUser);
        patient.setUser(savedUser);
        patientRepository.save(patient);

        return new ResponseEntity<>(
                ApiResponse.ok("Patient account registered and linked successfully", mapToResponse(savedUser)),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/register-doctor")
    public ResponseEntity<ApiResponse<UserResponse>> registerDoctor(
            @RequestBody DoctorRegistrationRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Doctor accounts can only be provisioned by hospital administrators."));
        }

        validateDoctorRegistrationRequest(request);

        String trimmedUsername = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(trimmedUsername)) {
            throw new DuplicateResourceException("Username '" + trimmedUsername + "' already exists.");
        }

        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new DuplicateResourceException("Email '" + trimmedEmail + "' is already registered.");
        }

        String trimmedMedReg = request.getMedicalRegistrationNumber().trim();
        if (userRepository.existsByMedicalRegistrationNumberIgnoreCase(trimmedMedReg)) {
            throw new DuplicateResourceException("Medical Registration Number '" + trimmedMedReg + "' is already registered.");
        }

        User doctor = new User();
        doctor.setUsername(trimmedUsername);
        doctor.setPassword(passwordEncoder.encode(request.getPassword()));
        doctor.setFullName(request.getFullName().trim());
        doctor.setRole(Role.ROLE_DOCTOR);
        doctor.setEmail(trimmedEmail);
        doctor.setSpecialization(request.getSpecialization().trim());
        doctor.setQualifications(request.getQualifications().trim());
        doctor.setMedicalRegistrationNumber(trimmedMedReg);
        doctor.setEnabled(false);
        doctor.setApprovalStatus(DoctorApprovalStatus.PENDING);
        doctor.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(doctor);

        return new ResponseEntity<>(
                ApiResponse.ok("Doctor registration submitted successfully. Your account is pending administrator approval before you can log in.", mapToResponse(saved)),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/doctors/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingDoctors() {
        List<User> pendingDoctors = userRepository.findByRoleAndApprovalStatus(Role.ROLE_DOCTOR, DoctorApprovalStatus.PENDING);
        List<UserResponse> responses = pendingDoctors.stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok("Pending doctor registrations retrieved successfully", responses));
    }

    @PatchMapping("/doctors/{id}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveDoctor(@PathVariable Long id) {
        User doctor = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor record with id " + id + " not found."));

        if (doctor.getRole() != Role.ROLE_DOCTOR) {
            throw new BadRequestException("Target user is not a doctor.");
        }

        doctor.setApprovalStatus(DoctorApprovalStatus.APPROVED);
        doctor.setEnabled(true);
        User saved = userRepository.save(doctor);

        return ResponseEntity.ok(ApiResponse.ok("Doctor account for '" + saved.getFullName() + "' approved successfully.", mapToResponse(saved)));
    }

    @PatchMapping("/doctors/{id}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectDoctor(@PathVariable Long id) {
        User doctor = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor record with id " + id + " not found."));

        if (doctor.getRole() != Role.ROLE_DOCTOR) {
            throw new BadRequestException("Target user is not a doctor.");
        }

        doctor.setApprovalStatus(DoctorApprovalStatus.REJECTED);
        doctor.setEnabled(false);
        User saved = userRepository.save(doctor);

        return ResponseEntity.ok(ApiResponse.ok("Doctor registration for '" + saved.getFullName() + "' has been rejected.", mapToResponse(saved)));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<UserResponse>> createStaffAccount(
            @RequestBody StaffAccountRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only ADMIN can create staff accounts."));
        }

        validateStaffAccountRequest(request);

        String trimmedUsername = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(trimmedUsername)) {
            throw new DuplicateResourceException("Username '" + trimmedUsername + "' already exists.");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String trimmedEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(trimmedEmail)) {
                throw new DuplicateResourceException("Email '" + trimmedEmail + "' is already registered.");
            }
        }

        if (request.getMedicalRegistrationNumber() != null && !request.getMedicalRegistrationNumber().trim().isEmpty()) {
            String trimmedMed = request.getMedicalRegistrationNumber().trim();
            if (userRepository.existsByMedicalRegistrationNumberIgnoreCase(trimmedMed)) {
                throw new DuplicateResourceException("Medical registration number '" + trimmedMed + "' is already registered.");
            }
        }

        User staff = new User();
        staff.setUsername(trimmedUsername);
        staff.setPassword(passwordEncoder.encode(request.getTemporaryPassword()));
        staff.setFullName(request.getFullName().trim());
        staff.setRole(request.getRole());
        staff.setEnabled(true);
        staff.setPasswordChangeRequired(true);
        staff.setApprovalStatus(DoctorApprovalStatus.APPROVED);
        staff.setEmail(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim().toLowerCase() : null);
        staff.setSpecialization(request.getSpecialization() != null && !request.getSpecialization().trim().isEmpty() ? request.getSpecialization().trim() : null);
        staff.setQualifications(request.getQualifications() != null && !request.getQualifications().trim().isEmpty() ? request.getQualifications().trim() : null);
        staff.setMedicalRegistrationNumber(request.getMedicalRegistrationNumber() != null && !request.getMedicalRegistrationNumber().trim().isEmpty() ? request.getMedicalRegistrationNumber().trim() : null);
        staff.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(staff);
        return new ResponseEntity<>(ApiResponse.ok("Staff account created successfully", mapToResponse(saved)), HttpStatus.CREATED);
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getStaffAccounts(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only ADMIN can view staff accounts."));
        }

        List<User> staff = userRepository.findByRoleIn(List.of(Role.ROLE_DOCTOR, Role.ROLE_RECEPTIONIST, Role.ROLE_ADMIN));
        List<UserResponse> responses = staff.stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok("Staff accounts retrieved successfully", responses));
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access Denied: Only ADMIN can toggle user status."));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authentication.getName().equalsIgnoreCase(user.getUsername())) {
            throw new BadRequestException("You cannot toggle your own active account status.");
        }

        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", mapToResponse(saved)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthenticated"));
        }

        if (request == null || request.getNewPassword() == null) {
            throw new BadRequestException("New password is required.");
        }
        PasswordValidator.validatePassword(request.getNewPassword());

        String username = authentication.getName();
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", "Password changed successfully"));
    }

    private void validateStaffAccountRequest(StaffAccountRequest request) {
        if (request == null) {
            throw new BadRequestException("Staff account request cannot be null.");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        if (request.getRole() == null || (request.getRole() != Role.ROLE_DOCTOR && request.getRole() != Role.ROLE_RECEPTIONIST)) {
            throw new BadRequestException("Role must be either ROLE_DOCTOR or ROLE_RECEPTIONIST.");
        }
        PasswordValidator.validatePassword(request.getTemporaryPassword());
        if (request.getRole() == Role.ROLE_DOCTOR) {
            if (request.getSpecialization() == null || request.getSpecialization().trim().isEmpty()) {
                throw new BadRequestException("Doctor specialization is required.");
            }
        }
    }

    private void validateDoctorRegistrationRequest(DoctorRegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be null.");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() || !request.getEmail().contains("@")) {
            throw new BadRequestException("A valid email address is required.");
        }
        if (request.getSpecialization() == null || request.getSpecialization().trim().isEmpty()) {
            throw new BadRequestException("Specialization is required.");
        }
        if (request.getQualifications() == null || request.getQualifications().trim().isEmpty()) {
            throw new BadRequestException("Qualifications are required.");
        }
        if (request.getMedicalRegistrationNumber() == null || request.getMedicalRegistrationNumber().trim().isEmpty()) {
            throw new BadRequestException("Medical registration number is required.");
        }
        PasswordValidator.validatePassword(request.getPassword());
    }

    private void validatePatientRegistrationRequest(PatientSelfRegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be null.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        PasswordValidator.validatePassword(request.getPassword());
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required.");
        }
        if (request.getPatientCode() == null || request.getPatientCode().trim().isEmpty()) {
            throw new BadRequestException("Patient Code (MRN) is required to link your record.");
        }
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be null.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        PasswordValidator.validatePassword(request.getPassword());
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required.");
        }
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getSpecialization(),
                user.getQualifications(),
                user.getMedicalRegistrationNumber(),
                user.getApprovalStatus(),
                user.isPasswordChangeRequired()
        );
    }
}

