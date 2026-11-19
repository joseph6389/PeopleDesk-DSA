package edu.college.employee.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {
    @AfterEach
    void clearAttempts() { AuthController.clearLoginAttemptsForTest(); }

    @Test
    void blocksAfterFiveFailuresAndReleasesAfterOneMinute() {
        long now = 1_000_000L;
        for (int attempt = 0; attempt < 5; attempt++) AuthController.recordFailedLoginForTest("Person", now + attempt);
        assertTrue(AuthController.loginBlocked("person", now + 5));
        assertFalse(AuthController.loginBlocked("PERSON", now + 60_005));
    }

    @Test
    void keepsCountersSeparateByUsernameAndExpiresOldFailureWindow() {
        long now = 2_000_000L;
        for (int attempt = 0; attempt < 4; attempt++) AuthController.recordFailedLoginForTest("person", now + attempt);
        assertFalse(AuthController.loginBlocked("other", now + 5));
        AuthController.recordFailedLoginForTest("person", now + 60_001);
        assertFalse(AuthController.loginBlocked("person", now + 60_002));
    }
}
