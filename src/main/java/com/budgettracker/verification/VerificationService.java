package com.budgettracker.verification;

import java.time.Instant;

import com.budgettracker.security.TokenUtil;
import com.budgettracker.user.User;
import com.budgettracker.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {

    public enum Status { SUCCESS, INVALID, EXPIRED }

    public record VerifyResult(Status status, ClientType client) {
    }

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final VerificationProperties props;
    private final ApplicationEventPublisher events;

    public VerificationService(EmailVerificationTokenRepository tokens, UserRepository users,
                               VerificationProperties props, ApplicationEventPublisher events) {
        this.tokens = tokens;
        this.users = users;
        this.props = props;
        this.events = events;
    }

    /** Replaces any older link with a fresh one and queues the email (sent after commit). */
    @Transactional
    public void sendVerification(User user, ClientType client) {
        tokens.deleteByUserId(user.getId());
        String raw = TokenUtil.newToken();
        tokens.save(new EmailVerificationToken(user.getId(), TokenUtil.sha256(raw),
                Instant.now().plus(props.expiration()), client));
        events.publishEvent(new VerificationEmailRequested(user.getEmail(),
                props.publicBaseUrl() + "/verify-email?token=" + raw));
    }

    @Transactional
    public VerifyResult verify(String rawToken) {
        EmailVerificationToken token = tokens.findByTokenHash(TokenUtil.sha256(rawToken)).orElse(null);
        User user = token == null ? null : users.findById(token.getUserId()).orElse(null);
        if (user == null) {
            return new VerifyResult(Status.INVALID, ClientType.WEB);
        }
        // Already verified (e.g. a mail scanner opened the link first, or a double click): still success.
        if (user.isEmailVerified()) {
            return new VerifyResult(Status.SUCCESS, token.getClient());
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            return new VerifyResult(Status.EXPIRED, token.getClient());
        }
        user.setEmailVerified(true);
        return new VerifyResult(Status.SUCCESS, token.getClient());
    }

    /**
     * Always silent: the caller cannot tell whether the email exists or is already verified.
     * At most one email per cooldown period per account.
     */
    @Transactional
    public void resend(String email, ClientType client) {
        users.findByEmail(email.trim().toLowerCase())
                .filter(u -> !u.isEmailVerified())
                .ifPresent(u -> {
                    boolean tooSoon = tokens.findFirstByUserIdOrderByCreatedAtDesc(u.getId())
                            .map(t -> t.getCreatedAt().plus(props.resendCooldown()).isAfter(Instant.now()))
                            .orElse(false);
                    if (!tooSoon) {
                        sendVerification(u, client);
                    }
                });
    }

    /** Where to send the browser after the link was clicked. */
    public String redirectUrl(VerifyResult result) {
        String base = result.client() == ClientType.ANDROID ? props.androidRedirectUrl() : props.webRedirectUrl();
        return base + (base.contains("?") ? "&" : "?") + "status=" + result.status().name().toLowerCase();
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void deleteExpired() {
        tokens.deleteExpired(Instant.now());
    }
}
