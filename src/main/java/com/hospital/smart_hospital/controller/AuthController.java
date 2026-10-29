package com.hospital.smart_hospital.controller;

import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.PatientSelfRegistrationRequest;
import com.hospital.smart_hospital.dto.RegisterRequest;
import com.hospital.smart_hospital.dto.UserResponse;
import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.exception.ResourceNotFoundException;
import com.hospital.smart_hospital.model.Patient;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User account is disabled"));
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

    private void validatePatientRegistrationRequest(PatientSelfRegistrationRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be null.");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long.");
        }
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
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long.");
        }
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
                user.getCreatedAt()
        );
    }
}

