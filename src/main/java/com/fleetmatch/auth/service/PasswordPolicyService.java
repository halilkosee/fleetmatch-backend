package com.fleetmatch.auth.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern NUMBER = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern REPEATED_CHARACTERS = Pattern.compile("(.)\\1{3,}");
    private static final Pattern REPEATED_GROUPS = Pattern.compile("(.{2,4})\\1{2,}");
    private static final Set<String> COMMON_WEAK_PASSWORDS = Set.of(
            "password",
            "password1",
            "password123",
            "qwerty",
            "qwerty123",
            "admin123",
            "welcome123",
            "letmein",
            "changeme",
            "easyfleetmatch",
            "devadmin"
    );

    public void validate(String password) {
        validate(password, null);
    }

    public void validate(String password, String email) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least 12 characters");
        }
        if (password == null || !UPPERCASE.matcher(password).find()) {
            errors.add("Password must include at least one uppercase letter");
        }
        if (password == null || !LOWERCASE.matcher(password).find()) {
            errors.add("Password must include at least one lowercase letter");
        }
        if (password == null || !NUMBER.matcher(password).find()) {
            errors.add("Password must include at least one number");
        }
        if (password == null || !SPECIAL.matcher(password).find()) {
            errors.add("Password must include at least one special character");
        }
        if (containsEmail(password, email)) {
            errors.add("Password must not contain your email");
        }
        if (hasObviousRepeatedPattern(password)) {
            errors.add("Password must not contain obvious repeated patterns");
        }
        if (isCommonWeakPassword(password)) {
            errors.add("Password is too common");
        }

        if (!errors.isEmpty()) {
            throw new BusinessRuleException(String.join("; ", errors));
        }
    }

    private boolean containsEmail(String password, String email) {
        if (password == null || email == null || email.isBlank()) {
            return false;
        }

        String normalizedPassword = password.toLowerCase();
        String normalizedEmail = email.toLowerCase();
        String localPart = normalizedEmail.split("@", 2)[0];

        return normalizedPassword.contains(normalizedEmail) ||
                (!localPart.isBlank() && normalizedPassword.contains(localPart));
    }

    private boolean hasObviousRepeatedPattern(String password) {
        if (password == null) {
            return false;
        }

        return REPEATED_CHARACTERS.matcher(password).find() ||
                REPEATED_GROUPS.matcher(password).find();
    }

    private boolean isCommonWeakPassword(String password) {
        if (password == null) {
            return false;
        }

        String normalized = password
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");

        return COMMON_WEAK_PASSWORDS.contains(normalized);
    }
}
