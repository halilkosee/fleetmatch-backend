package com.fleetmatch.auth.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PasswordPolicyService {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern NUMBER = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    public void validate(String password) {
        if (password == null ||
                password.length() < 8 ||
                !UPPERCASE.matcher(password).find() ||
                !LOWERCASE.matcher(password).find() ||
                !NUMBER.matcher(password).find() ||
                !SPECIAL.matcher(password).find()) {

            throw new BusinessRuleException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number and special character"
            );
        }
    }
}
