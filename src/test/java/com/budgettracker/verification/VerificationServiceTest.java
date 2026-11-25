package com.budgettracker.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.budgettracker.security.TokenUtil;
import com.budgettracker.user.User;
import com.budgettracker.user.UserRepository;
import com.budgettracker.verification.VerificationService.Status;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class VerificationServiceTest {

    private final EmailVerificationTokenRepository tokens = mock(EmailVerificationTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final VerificationService service = new VerificationService(tokens, users,
            new VerificationProperties(Duration.ofHours(24), Duration.ofSeconds(60), "http://api",
                    "http://web/done", "myapp://done"), events);

    private final User user = new User("a@x.com", "hash");

    private EmailVerificationToken tokenExpiringIn(Duration d, ClientType client) {
        return new EmailVerificationToken(1L, TokenUtil.sha256("raw"), Instant.now().plus(d), client);
    }

    @Test
    void validTokenVerifiesUser() {
        when(tokens.findByTokenHash(TokenUtil.sha256("raw"))).thenReturn(Optional.of(tokenExpiringIn(Duration.ofHours(1), ClientType.ANDROID)));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        var result = service.verify("raw");

        assertEquals(Status.SUCCESS, result.status());
        assertTrue(user.isEmailVerified());
        assertEquals("myapp://done?status=success", service.redirectUrl(result));
    }

    @Test
    void expiredTokenDoesNotVerify() {
        when(tokens.findByTokenHash(TokenUtil.sha256("raw"))).thenReturn(Optional.of(tokenExpiringIn(Duration.ofHours(-1), ClientType.WEB)));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        assertEquals(Status.EXPIRED, service.verify("raw").status());
        assertFalse(user.isEmailVerified());
    }

    @Test
    void unknownTokenIsInvalidAndRedirectsToWeb() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());
        var result = service.verify("nope");
        assertEquals(Status.INVALID, result.status());
        assertEquals("http://web/done?status=invalid", service.redirectUrl(result));
    }

    @Test
    void secondClickOnAlreadyVerifiedAccountStillSucceeds() {
        user.setEmailVerified(true);
        when(tokens.findByTokenHash(TokenUtil.sha256("raw"))).thenReturn(Optional.of(tokenExpiringIn(Duration.ofHours(-5), ClientType.WEB)));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertEquals(Status.SUCCESS, service.verify("raw").status());
    }

    @Test
    void resendIsSkippedDuringCooldownAndForUnknownOrVerifiedEmails() {
        when(users.findByEmail("a@x.com")).thenReturn(Optional.of(user));
        when(tokens.findFirstByUserIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(tokenExpiringIn(Duration.ofHours(24), ClientType.WEB)));
        service.resend("A@x.com", ClientType.WEB); // token created "just now" -> cooldown
        when(users.findByEmail("nobody@x.com")).thenReturn(Optional.empty());
        service.resend("nobody@x.com", ClientType.WEB);
        verify(events, never()).publishEvent(any(Object.class));
    }
}
