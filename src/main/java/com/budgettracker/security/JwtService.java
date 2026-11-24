package com.budgettracker.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.budgettracker.user.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public record TokenData(long userId, Role role) {
    }

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(JwtProperties props) {
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (set JWT_SECRET)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMillis = props.expiration().toMillis();
    }

    public long expiresInSeconds() {
        return expirationMillis / 1000;
    }

    public String generateToken(long userId, Role role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("role", role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(key)
                .compact();
    }

    /** Returns the token contents if it is valid and not expired, otherwise empty. */
    public Optional<TokenData> parse(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new TokenData(Long.parseLong(claims.getSubject()),
                    Role.valueOf(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
