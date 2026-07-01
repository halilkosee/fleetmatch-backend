package com.fleetmatch.auth.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService();

    @Test
    void strongPasswordIsAccepted() {
        assertDoesNotThrow(() ->
                service.validate("StrongerPass!2026", "owner@example.com")
        );
    }

    @Test
    void weakPasswordReturnsClearValidationMessages() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("short", "owner@example.com")
        );

        assertTrue(exception.getMessage().contains("at least 12 characters"));
        assertTrue(exception.getMessage().contains("uppercase"));
        assertTrue(exception.getMessage().contains("number"));
        assertTrue(exception.getMessage().contains("special character"));
    }

    @Test
    void passwordCannotContainEmailOrLocalPart() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("Owner@example.com!123", "owner@example.com")
        );

        assertTrue(exception.getMessage().contains("must not contain your email"));
    }

    @Test
    void obviousRepeatedPatternsAreRejected() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("AAAA1111bbbb!", "driver@example.com")
        );

        assertTrue(exception.getMessage().contains("repeated patterns"));
    }

    @Test
    void commonWeakPasswordsAreRejectedWhenKnown() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> service.validate("Password123!", "driver@example.com")
        );

        assertTrue(exception.getMessage().contains("too common"));
    }
}
