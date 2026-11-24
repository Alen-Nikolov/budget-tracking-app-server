package com.budgettracker.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import com.budgettracker.user.Role;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void roundTripsUserId() {
        JwtService jwt = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
        var data = jwt.parse(jwt.generateToken(42L, Role.ADMIN)).orElseThrow();
        assertEquals(42L, data.userId());
        assertEquals(Role.ADMIN, data.role());
    }

    @Test
    void rejectsExpiredAndTamperedTokens() {
        JwtService expired = new JwtService(new JwtProperties(SECRET, Duration.ofMillis(-1000)));
        assertTrue(expired.parse(expired.generateToken(1L, Role.USER)).isEmpty());

        JwtService jwt = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
        assertTrue(jwt.parse(jwt.generateToken(1L, Role.USER) + "x").isEmpty());
        assertTrue(jwt.parse("garbage").isEmpty());
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService(new JwtProperties("short", Duration.ofHours(1))));
    }
}
