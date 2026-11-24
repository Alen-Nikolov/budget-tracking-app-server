package com.budgettracker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.budgettracker.security.RefreshTokenProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final RefreshTokenProperties props;

    public RefreshTokenService(RefreshTokenRepository repo, RefreshTokenProperties props) {
        this.repo = repo;
        this.props = props;
    }

    /** Creates a new refresh token and returns the raw value (only its hash is stored). */
    @Transactional
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repo.save(new RefreshToken(userId, hash(raw), Instant.now().plus(props.expiration())));
        return raw;
    }

    /**
     * Validates and consumes a refresh token (single use) and returns the owning user id.
     * Presenting an already-used token means it was probably stolen, so ALL of that user's
     * refresh tokens are revoked. noRollbackFor keeps that revocation from being rolled back.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Long consume(String raw) {
        RefreshToken token = repo.findForUpdate(hash(raw)).orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();
        if (token.getRevokedAt() != null) {
            repo.revokeAllForUser(token.getUserId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException();
        }
        token.revoke();
        return token.getUserId();
    }

    /** Logout: revoke this one token. Unknown tokens are ignored. */
    @Transactional
    public void revoke(String raw) {
        repo.findForUpdate(hash(raw)).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.revoke();
            }
        });
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpired() {
        repo.deleteExpired(Instant.now());
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
