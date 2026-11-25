package com.budgettracker.auth;

import java.time.Instant;

import com.budgettracker.security.RefreshTokenProperties;
import com.budgettracker.security.TokenUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final RefreshTokenProperties props;

    public RefreshTokenService(RefreshTokenRepository repo, RefreshTokenProperties props) {
        this.repo = repo;
        this.props = props;
    }

    /** Creates a new refresh token and returns the raw value (only its hash is stored). */
    @Transactional
    public String issue(Long userId) {
        String raw = TokenUtil.newToken();
        repo.save(new RefreshToken(userId, TokenUtil.sha256(raw), Instant.now().plus(props.expiration())));
        return raw;
    }

    /**
     * Validates and consumes a refresh token (single use) and returns the owning user id.
     * Presenting an already-used token means it was probably stolen, so ALL of that user's
     * refresh tokens are revoked. noRollbackFor keeps that revocation from being rolled back.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Long consume(String raw) {
        RefreshToken token = repo.findForUpdate(TokenUtil.sha256(raw)).orElseThrow(InvalidRefreshTokenException::new);
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
        repo.findForUpdate(TokenUtil.sha256(raw)).ifPresent(t -> {
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

}
