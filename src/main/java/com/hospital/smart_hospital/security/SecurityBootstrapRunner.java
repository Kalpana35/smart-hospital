package com.hospital.smart_hospital.security;

import com.hospital.smart_hospital.model.Role;
import com.hospital.smart_hospital.model.User;
import com.hospital.smart_hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SecurityBootstrapRunner.class);
    private static final String ADMIN_PASSWORD_ENV_VAR = "SMART_HOSPITAL_ADMIN_PASSWORD";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public SecurityBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        String adminPassword = resolveAdminPassword();

        Optional<User> existingAdmin = userRepository.findByUsernameIgnoreCase("admin");

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();

            if (adminPassword != null && !adminPassword.isEmpty()) {
                // Safely update/reset the existing admin's password
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setEnabled(true);
                admin.setRole(Role.ROLE_ADMIN);
                userRepository.save(admin);
                logger.info("Security bootstrap: Existing ADMIN account ('admin') password has been updated/reset successfully from environment configuration.");
            } else {
                logger.info("Security bootstrap: ADMIN account ('admin') already exists in the database. Set '{}' to reset its password.", ADMIN_PASSWORD_ENV_VAR);
            }
            return;
        }

        // If no 'admin' account exists yet
        if (adminPassword != null && !adminPassword.isEmpty()) {
            User initialAdmin = new User(
                    "admin",
                    passwordEncoder.encode(adminPassword),
                    "System Administrator",
                    Role.ROLE_ADMIN
            );
            initialAdmin.setEnabled(true);
            userRepository.save(initialAdmin);
            logger.info("Security bootstrap: Initial ADMIN account ('admin') has been created successfully from environment configuration.");
        } else {
            logger.warn("Security bootstrap: No ADMIN account exists in the database and environment variable '{}' is not set. "
                    + "To create or reset the administrator account, set '{}' and restart the application.",
                    ADMIN_PASSWORD_ENV_VAR, ADMIN_PASSWORD_ENV_VAR);
        }
    }

    /**
     * Resolves the admin password from environment variables, Spring environment properties,
     * or JVM system properties.
     */
    private String resolveAdminPassword() {
        String val = environment.getProperty(ADMIN_PASSWORD_ENV_VAR);
        if (val == null || val.trim().isEmpty()) {
            val = environment.getProperty("smart.hospital.admin.password");
        }
        if (val == null || val.trim().isEmpty()) {
            val = System.getenv(ADMIN_PASSWORD_ENV_VAR);
        }
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty("smart.hospital.admin.password");
        }
        if (val == null || val.trim().isEmpty()) {
            val = System.getProperty(ADMIN_PASSWORD_ENV_VAR);
        }
        return cleanPassword(val);
    }

    /**
     * Sanitizes password input by trimming whitespace and stripping accidental
     * enclosing quotation marks introduced by Windows shells (CMD/PowerShell).
     */
    private String cleanPassword(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }
}

