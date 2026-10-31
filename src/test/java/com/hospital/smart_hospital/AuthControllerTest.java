package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.AuthController;
import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.ChangePasswordRequest;
import com.hospital.smart_hospital.dto.DoctorRegistrationRequest;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.RegisterRequest;
import com.hospital.smart_hospital.dto.StaffAccountRequest;
import com.hospital.smart_hospital.dto.UserResponse;
import com.hospital.smart_hospital.exception.DuplicateResourceException;
import com.hospital.smart_hospital.model.DoctorApprovalStatus;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("admin", "$2a$10$encodedPassword", "System Admin", Role.ROLE_ADMIN);
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("admin", "Secret@123");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        Authentication auth = mock(Authentication.class);

        when(httpRequest.getSession(true)).thenReturn(session);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getUsername()).isEqualTo("admin");
        assertThat(response.getBody().getData().getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when credentials are invalid")
    void testLogin_BadCredentials() {
        LoginRequest request = new LoginRequest("admin", "WrongPassword");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Invalid username or password");
    }

    @Test
    @DisplayName("Should retrieve current authenticated user details")
    void testGetCurrentUser_Success() {
        Authentication auth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(sampleUser));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.getCurrentUser(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should allow ADMIN to register a DOCTOR account")
    void testRegisterUser_ByAdmin_Success() {
        RegisterRequest registerRequest = new RegisterRequest("dr.smith", "DoctorPass@123", "Dr. John Smith", Role.ROLE_DOCTOR);
        Authentication adminAuth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("dr.smith")).thenReturn(false);
        when(passwordEncoder.encode("DoctorPass@123")).thenReturn("$2a$10$encodedHash");

        User doctorUser = new User("dr.smith", "$2a$10$encodedHash", "Dr. John Smith", Role.ROLE_DOCTOR);
        doctorUser.setId(2L);
        when(userRepository.save(any(User.class))).thenReturn(doctorUser);

        ResponseEntity<ApiResponse<UserResponse>> response = authController.registerUser(registerRequest, adminAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getUsername()).isEqualTo("dr.smith");
        assertThat(response.getBody().getData().getRole()).isEqualTo(Role.ROLE_DOCTOR);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject user registration when requester is not an ADMIN")
    void testRegisterUser_NotAdmin_Forbidden() {
        RegisterRequest registerRequest = new RegisterRequest("dr.smith", "DoctorPass123", "Dr. John Smith", Role.ROLE_DOCTOR);
        Authentication nonAdminAuth = new TestingAuthenticationToken("doctor", "Secret@123", "ROLE_DOCTOR");

        ResponseEntity<ApiResponse<UserResponse>> response = authController.registerUser(registerRequest, nonAdminAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("Only ADMIN can register new accounts");
    }

    @Test
    @DisplayName("Should return 401 with clear pending message when pending doctor attempts to log in")
    void testLogin_Disabled_PendingDoctor() {
        LoginRequest request = new LoginRequest("dr.pending", "SecretPass123");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        User pendingDoc = new User("dr.pending", "$2a$10$hashed", "Dr. Pending Approval", Role.ROLE_DOCTOR);
        pendingDoc.setEnabled(false);
        pendingDoc.setApprovalStatus(DoctorApprovalStatus.PENDING);
        when(userRepository.findByUsernameIgnoreCase("dr.pending")).thenReturn(Optional.of(pendingDoc));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("pending administrator approval");
    }

    @Test
    @DisplayName("Should return 401 with clear rejected message when rejected doctor attempts to log in")
    void testLogin_Disabled_RejectedDoctor() {
        LoginRequest request = new LoginRequest("dr.rejected", "SecretPass123");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User account is disabled"));

        User rejectedDoc = new User("dr.rejected", "$2a$10$hashed", "Dr. Rejected Access", Role.ROLE_DOCTOR);
        rejectedDoc.setEnabled(false);
        rejectedDoc.setApprovalStatus(DoctorApprovalStatus.REJECTED);
        when(userRepository.findByUsernameIgnoreCase("dr.rejected")).thenReturn(Optional.of(rejectedDoc));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("rejected");
    }

    @Test
    @DisplayName("Should register doctor with PENDING approval status and disabled account when called by ADMIN")
    void testRegisterDoctor_Success() {
        DoctorRegistrationRequest request = new DoctorRegistrationRequest(
                "Dr. Sarah Connor",
                "dr.sarah",
                "sarah.connor@hospital.com",
                "Cardiology",
                "MBBS, MD (Cardio)",
                "MED-REG-98765",
                "DoctorPass@123"
        );
        Authentication adminAuth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("dr.sarah")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("sarah.connor@hospital.com")).thenReturn(false);
        when(userRepository.existsByMedicalRegistrationNumberIgnoreCase("MED-REG-98765")).thenReturn(false);
        when(passwordEncoder.encode("DoctorPass@123")).thenReturn("$2a$10$encodedPass");

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setUsername("dr.sarah");
        savedUser.setFullName("Dr. Sarah Connor");
        savedUser.setEmail("sarah.connor@hospital.com");
        savedUser.setSpecialization("Cardiology");
        savedUser.setQualifications("MBBS, MD (Cardio)");
        savedUser.setMedicalRegistrationNumber("MED-REG-98765");
        savedUser.setRole(Role.ROLE_DOCTOR);
        savedUser.setEnabled(false);
        savedUser.setApprovalStatus(DoctorApprovalStatus.PENDING);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ResponseEntity<ApiResponse<UserResponse>> response = authController.registerDoctor(request, adminAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).contains("pending administrator approval");
        assertThat(response.getBody().getData().getApprovalStatus()).isEqualTo(DoctorApprovalStatus.PENDING);
        assertThat(response.getBody().getData().isEnabled()).isFalse();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject doctor registration when unauthenticated or non-admin")
    void testRegisterDoctor_ForbiddenForPublic() {
        DoctorRegistrationRequest request = new DoctorRegistrationRequest(
                "Dr. Sarah Connor",
                "dr.sarah",
                "sarah.connor@hospital.com",
                "Cardiology",
                "MBBS, MD (Cardio)",
                "MED-REG-98765",
                "DoctorPass@123"
        );

        ResponseEntity<ApiResponse<UserResponse>> response = authController.registerDoctor(request, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("Doctor accounts can only be provisioned by hospital administrators");
    }

    @Test
    @DisplayName("Should reject doctor registration when username already exists")
    void testRegisterDoctor_DuplicateUsername() {
        DoctorRegistrationRequest request = new DoctorRegistrationRequest(
                "Dr. Sarah Connor",
                "dr.sarah",
                "sarah.connor@hospital.com",
                "Cardiology",
                "MBBS, MD (Cardio)",
                "MED-REG-98765",
                "DoctorPass@123"
        );
        Authentication adminAuth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("dr.sarah")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authController.registerDoctor(request, adminAuth));
    }

    @Test
    @DisplayName("Should reject doctor registration when email already exists")
    void testRegisterDoctor_DuplicateEmail() {
        DoctorRegistrationRequest request = new DoctorRegistrationRequest(
                "Dr. Sarah Connor",
                "dr.sarah",
                "sarah.connor@hospital.com",
                "Cardiology",
                "MBBS, MD (Cardio)",
                "MED-REG-98765",
                "DoctorPass@123"
        );
        Authentication adminAuth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("dr.sarah")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("sarah.connor@hospital.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authController.registerDoctor(request, adminAuth));
    }

    @Test
    @DisplayName("Should reject doctor registration when medical registration number already exists")
    void testRegisterDoctor_DuplicateMedicalRegistrationNumber() {
        DoctorRegistrationRequest request = new DoctorRegistrationRequest(
                "Dr. Sarah Connor",
                "dr.sarah",
                "sarah.connor@hospital.com",
                "Cardiology",
                "MBBS, MD (Cardio)",
                "MED-REG-98765",
                "DoctorPass@123"
        );
        Authentication adminAuth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("dr.sarah")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("sarah.connor@hospital.com")).thenReturn(false);
        when(userRepository.existsByMedicalRegistrationNumberIgnoreCase("MED-REG-98765")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authController.registerDoctor(request, adminAuth));
    }

    @Test
    @DisplayName("Should retrieve list of pending doctor registrations")
    void testGetPendingDoctors() {
        User pendingDoc = new User("dr.pending", "$2a$10$hash", "Dr. Pending", Role.ROLE_DOCTOR);
        pendingDoc.setId(11L);
        pendingDoc.setEnabled(false);
        pendingDoc.setApprovalStatus(DoctorApprovalStatus.PENDING);

        when(userRepository.findByRoleAndApprovalStatus(Role.ROLE_DOCTOR, DoctorApprovalStatus.PENDING))
                .thenReturn(List.of(pendingDoc));

        ResponseEntity<ApiResponse<List<UserResponse>>> response = authController.getPendingDoctors();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getUsername()).isEqualTo("dr.pending");
    }

    @Test
    @DisplayName("Should approve pending doctor, setting status to APPROVED and enabled to true")
    void testApproveDoctor_Success() {
        User pendingDoc = new User("dr.pending", "$2a$10$hash", "Dr. Pending", Role.ROLE_DOCTOR);
        pendingDoc.setId(12L);
        pendingDoc.setEnabled(false);
        pendingDoc.setApprovalStatus(DoctorApprovalStatus.PENDING);

        when(userRepository.findById(12L)).thenReturn(Optional.of(pendingDoc));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.approveDoctor(12L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getApprovalStatus()).isEqualTo(DoctorApprovalStatus.APPROVED);
        assertThat(response.getBody().getData().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should reject pending doctor, setting status to REJECTED and enabled to false")
    void testRejectDoctor_Success() {
        User pendingDoc = new User("dr.pending", "$2a$10$hash", "Dr. Pending", Role.ROLE_DOCTOR);
        pendingDoc.setId(13L);
        pendingDoc.setEnabled(false);
        pendingDoc.setApprovalStatus(DoctorApprovalStatus.PENDING);

        when(userRepository.findById(13L)).thenReturn(Optional.of(pendingDoc));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ApiResponse<UserResponse>> response = authController.rejectDoctor(13L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getApprovalStatus()).isEqualTo(DoctorApprovalStatus.REJECTED);
        assertThat(response.getBody().getData().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Admin can create a receptionist staff account with temporary password and passwordChangeRequired=true")
    void testCreateStaffAccount_Receptionist_Success() {
        StaffAccountRequest request = new StaffAccountRequest(
                "Jane Desk",
                "jane.receptionist",
                "jane@hospital.com",
                Role.ROLE_RECEPTIONIST,
                "TempPass@123",
                null,
                null,
                null
        );

        Authentication adminAuth = new TestingAuthenticationToken("admin", "pwd", "ROLE_ADMIN");
        when(userRepository.existsByUsernameIgnoreCase("jane.receptionist")).thenReturn(false);
        when(passwordEncoder.encode("TempPass@123")).thenReturn("encodedTempPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(50L);
            return u;
        });

        ResponseEntity<ApiResponse<UserResponse>> response = authController.createStaffAccount(request, adminAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getRole()).isEqualTo(Role.ROLE_RECEPTIONIST);
        assertThat(response.getBody().getData().isPasswordChangeRequired()).isTrue();
        assertThat(response.getBody().getData().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Authenticated user can change password and clear passwordChangeRequired flag")
    void testChangePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldSecret", "NewSecret@123");
        Authentication userAuth = new TestingAuthenticationToken("user1", "oldSecret", "ROLE_RECEPTIONIST");

        User user = new User("user1", "encodedOld", "User One", Role.ROLE_RECEPTIONIST);
        user.setPasswordChangeRequired(true);

        when(userRepository.findByUsernameIgnoreCase("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldSecret", "encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("NewSecret@123")).thenReturn("encodedNew");

        ResponseEntity<ApiResponse<String>> response = authController.changePassword(request, userAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(user.isPasswordChangeRequired()).isFalse();
        verify(userRepository).save(user);
    }
}

