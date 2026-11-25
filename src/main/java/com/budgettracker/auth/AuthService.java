package com.budgettracker.auth;

import com.budgettracker.auth.AuthDtos.AuthResponse;
import com.budgettracker.auth.AuthDtos.LoginRequest;
import com.budgettracker.auth.AuthDtos.RegisterRequest;
import com.budgettracker.auth.AuthDtos.RegistrationResponse;
import com.budgettracker.common.ConflictException;
import com.budgettracker.security.JwtService;
import com.budgettracker.user.User;
import com.budgettracker.user.UserRepository;
import com.budgettracker.verification.ClientType;
import com.budgettracker.verification.VerificationService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final LoginRateLimiter rateLimiter;
    private final VerificationService verification;

    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authenticationManager,
                       JwtService jwt, RefreshTokenService refreshTokens, LoginRateLimiter rateLimiter,
                       VerificationService verification) {
        this.users = users;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.rateLimiter = rateLimiter;
        this.verification = verification;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = users.save(new User(email, encoder.encode(req.password())));
        verification.sendVerification(user, req.client() != null ? req.client() : ClientType.WEB);
        return new RegistrationResponse("Verification email sent. Please check your inbox.", user.getEmail());
    }

    public AuthResponse login(LoginRequest req, String clientIp) {
        String email = normalize(req.email());
        rateLimiter.checkAllowed(email, clientIp); // 429 before we even look at the password
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        } catch (AuthenticationException e) {
            rateLimiter.recordFailure(email, clientIp);
            throw e; // -> generic 401
        }
        rateLimiter.reset(email);
        User user = users.findByEmail(email).orElseThrow();
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return respond(user);
    }

    /** Swaps a valid refresh token for a new access token + a NEW refresh token (rotation). */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(String refreshToken) {
        Long userId = refreshTokens.consume(refreshToken);
        // Re-read the user so role changes apply on refresh, and deleted users cannot refresh.
        User user = users.findById(userId).orElseThrow(InvalidRefreshTokenException::new);
        return respond(user);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(jwt.generateToken(user.getId(), user.getRole()), jwt.expiresInSeconds(),
                refreshTokens.issue(user.getId()), user.getId(), user.getEmail(), user.getRole());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
