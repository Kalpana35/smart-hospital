package com.hospital.smart_hospital;

import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import com.hospital.smart_hospital.security.SecurityBootstrapRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    @Test
    @DisplayName("Should create initial Admin account when none exists and password is provided")
    void testCreateInitialAdmin() {
        when(environment.getProperty("SMART_HOSPITAL_ADMIN_PASSWORD")).thenReturn("Admin@123");
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin@123")).thenReturn("$2a$10$encodedHash123");

        SecurityBootstrapRunner runner = new SecurityBootstrapRunner(userRepository, passwordEncoder, environment);
        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("admin");
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$encodedHash123");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(savedUser.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should safely reset existing Admin password without duplicate user creation")
    void testResetExistingAdminPassword() {
        User existingAdmin = new User("admin", "$2a$10$oldHash", "System Administrator", Role.ROLE_ADMIN);
        existingAdmin.setId(1L);

        when(environment.getProperty("SMART_HOSPITAL_ADMIN_PASSWORD")).thenReturn("NewAdminPass@456");
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(existingAdmin));
        when(passwordEncoder.encode("NewAdminPass@456")).thenReturn("$2a$10$newEncodedHash456");

        SecurityBootstrapRunner runner = new SecurityBootstrapRunner(userRepository, passwordEncoder, environment);
        runner.run();

        verify(userRepository).save(existingAdmin);
        assertThat(existingAdmin.getPassword()).isEqualTo("$2a$10$newEncodedHash456");
        assertThat(existingAdmin.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should strip accidental quotes introduced by Windows command shells")
    void testStripEnclosingQuotes() {
        User existingAdmin = new User("admin", "$2a$10$oldHash", "System Administrator", Role.ROLE_ADMIN);
        existingAdmin.setId(1L);

        when(environment.getProperty("SMART_HOSPITAL_ADMIN_PASSWORD")).thenReturn("\"QuotedPassword@789\"");
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(existingAdmin));
        when(passwordEncoder.encode("QuotedPassword@789")).thenReturn("$2a$10$cleanedHash");

        SecurityBootstrapRunner runner = new SecurityBootstrapRunner(userRepository, passwordEncoder, environment);
        runner.run();

        verify(passwordEncoder).encode("QuotedPassword@789");
        assertThat(existingAdmin.getPassword()).isEqualTo("$2a$10$cleanedHash");
    }

    @Test
    @DisplayName("Should not alter admin when no password environment variable is provided")
    void testNoPasswordProvided_LeavesAdminUnchanged() {
        User existingAdmin = new User("admin", "$2a$10$existingHash", "System Administrator", Role.ROLE_ADMIN);
        existingAdmin.setId(1L);

        when(environment.getProperty(anyString())).thenReturn(null);
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(existingAdmin));

        SecurityBootstrapRunner runner = new SecurityBootstrapRunner(userRepository, passwordEncoder, environment);
        runner.run();

        verify(userRepository, never()).save(any(User.class));
        assertThat(existingAdmin.getPassword()).isEqualTo("$2a$10$existingHash");
    }
}

