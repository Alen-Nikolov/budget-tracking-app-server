package com.budgettracker.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.budgettracker.common.TooManyRequestsException;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    /** Mutable clock so the test can jump forward in time. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final TestClock clock = new TestClock();
    private final LoginRateLimiter limiter =
            new LoginRateLimiter(new LoginRateLimitProperties(5, 20, Duration.ofHours(1)), clock);

    @Test
    void blocksAfterFiveFailuresThenAllowsAfterWindow() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.checkAllowed("a@x.com", "1.1.1.1"));
            limiter.recordFailure("a@x.com", "1.1.1.1");
        }
        var ex = assertThrows(TooManyRequestsException.class, () -> limiter.checkAllowed("a@x.com", "9.9.9.9"));
        assertEquals(3600, ex.getRetryAfterSeconds());

        clock.now = clock.now.plus(Duration.ofMinutes(61));
        assertDoesNotThrow(() -> limiter.checkAllowed("a@x.com", "1.1.1.1"));
    }

    @Test
    void otherEmailsAreUnaffectedAndSuccessResets() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("a@x.com", "1.1.1." + i);
        }
        assertDoesNotThrow(() -> limiter.checkAllowed("b@x.com", "2.2.2.2"));
        limiter.reset("a@x.com");
        assertDoesNotThrow(() -> limiter.checkAllowed("a@x.com", "3.3.3.3"));
    }

    @Test
    void blocksOneIpSprayingManyEmails() {
        for (int i = 0; i < 20; i++) {
            limiter.recordFailure("user" + i + "@x.com", "6.6.6.6");
        }
        assertThrows(TooManyRequestsException.class, () -> limiter.checkAllowed("fresh@x.com", "6.6.6.6"));
    }
}
