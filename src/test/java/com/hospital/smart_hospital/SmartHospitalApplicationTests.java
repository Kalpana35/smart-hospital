package com.hospital.smart_hospital;

import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SmartHospitalApplicationTests {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        assertThat(authenticationManager).isNotNull();
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    void testAuthenticationManager_AuthenticatesUser() {
        // Create a test user in DB
        String testUsername = "test_auth_user";
        String rawPassword = "TestSecretPassword@123";
        userRepository.findByUsernameIgnoreCase(testUsername).ifPresent(userRepository::delete);

        User testUser = new User(testUsername, passwordEncoder.encode(rawPassword), "Test User", Role.ROLE_ADMIN);
        userRepository.save(testUser);

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(testUsername, rawPassword)
            );

            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getName()).isEqualTo(testUsername);
        } finally {
            userRepository.delete(testUser);
        }
    }
}
