package com.hospital.smart_hospital;

import com.hospital.smart_hospital.exception.BadRequestException;
import com.hospital.smart_hospital.util.PasswordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Reception@123",
            "Doctor#2026",
            "Hospital$99",
            "Admin!Pass1",
            "Complex*Password9",
            "Secure_Pass7"
    })
    @DisplayName("Should validate successfully for compliant strong passwords")
    void testValidPasswords(String password) {
        assertThat(PasswordValidator.isValid(password)).isTrue();
        assertThat(PasswordValidator.getValidationErrors(password)).isEmpty();
        // Should not throw exception
        PasswordValidator.validatePassword(password);
    }

    @Test
    @DisplayName("Should detect missing uppercase letter")
    void testMissingUppercase() {
        String pwd = "reception@123";
        List<String> errors = PasswordValidator.getValidationErrors(pwd);
        assertThat(errors).anyMatch(e -> e.contains("uppercase letter"));
        assertThatThrownBy(() -> PasswordValidator.validatePassword(pwd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("uppercase letter");
    }

    @Test
    @DisplayName("Should detect missing lowercase letter")
    void testMissingLowercase() {
        String pwd = "RECEPTION@123";
        List<String> errors = PasswordValidator.getValidationErrors(pwd);
        assertThat(errors).anyMatch(e -> e.contains("lowercase letter"));
        assertThatThrownBy(() -> PasswordValidator.validatePassword(pwd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lowercase letter");
    }

    @Test
    @DisplayName("Should detect missing digit")
    void testMissingDigit() {
        String pwd = "Reception@Pass";
        List<String> errors = PasswordValidator.getValidationErrors(pwd);
        assertThat(errors).anyMatch(e -> e.contains("number"));
        assertThatThrownBy(() -> PasswordValidator.validatePassword(pwd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("number");
    }

    @Test
    @DisplayName("Should detect missing special character")
    void testMissingSpecialChar() {
        String pwd = "Reception123";
        List<String> errors = PasswordValidator.getValidationErrors(pwd);
        assertThat(errors).anyMatch(e -> e.contains("special character"));
        assertThatThrownBy(() -> PasswordValidator.validatePassword(pwd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("special character");
    }

    @Test
    @DisplayName("Should detect password that is too short")
    void testTooShort() {
        String pwd = "Rec@1";
        List<String> errors = PasswordValidator.getValidationErrors(pwd);
        assertThat(errors).anyMatch(e -> e.contains("at least 6 characters"));
        assertThatThrownBy(() -> PasswordValidator.validatePassword(pwd))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 6 characters");
    }

    @Test
    @DisplayName("Should detect null or empty password")
    void testNullOrEmpty() {
        assertThat(PasswordValidator.isValid(null)).isFalse();
        assertThat(PasswordValidator.isValid("   ")).isFalse();
        assertThatThrownBy(() -> PasswordValidator.validatePassword(null))
                .isInstanceOf(BadRequestException.class);
    }
}
