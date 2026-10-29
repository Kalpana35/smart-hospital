package com.hospital.smart_hospital;

import com.hospital.smart_hospital.controller.AuthController;
import com.hospital.smart_hospital.dto.ApiResponse;
import com.hospital.smart_hospital.dto.LoginRequest;
import com.hospital.smart_hospital.dto.RegisterRequest;
import com.hospital.smart_hospital.dto.UserResponse;
import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.PatientRepository;
import com.hospital.smart_hospital.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
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
        RegisterRequest registerRequest = new RegisterRequest("dr.smith", "DoctorPass123", "Dr. John Smith", Role.ROLE_DOCTOR);
        Authentication adminAuth = new TestingAuthenticationToken("admin", "Secret@123", "ROLE_ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("dr.smith")).thenReturn(false);
        when(passwordEncoder.encode("DoctorPass123")).thenReturn("$2a$10$encodedHash");

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
}

