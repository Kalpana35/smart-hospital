package com.hospital.smart_hospital.util;

import com.hospital.smart_hospital.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;

public final class PasswordValidator {

    private static final String SPECIAL_CHARS_REGEX = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*";

    private PasswordValidator() {
    }

    public static List<String> getValidationErrors(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.trim().isEmpty()) {
            errors.add("Password is required.");
            return errors;
        }

        if (password.length() < 6) {
            errors.add("Password must be at least 6 characters long.");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            errors.add("Password must contain at least one uppercase letter (A-Z).");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            errors.add("Password must contain at least one lowercase letter (a-z).");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            errors.add("Password must contain at least one number (0-9).");
        }
        if (!password.matches(SPECIAL_CHARS_REGEX)) {
            errors.add("Password must contain at least one special character (e.g. @, #, $, !).");
        }

        return errors;
    }

    public static boolean isValid(String password) {
        return getValidationErrors(password).isEmpty();
    }

    public static void validatePassword(String password) {
        List<String> errors = getValidationErrors(password);
        if (!errors.isEmpty()) {
            throw new BadRequestException("Password does not meet security requirements: "
                    + String.join(" ", errors)
                    + " (Example valid password: Reception@123)");
        }
    }
}
