package com.budgettracker.auth;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import com.budgettracker.common.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sliding-window limiter on FAILED logins, per email and per IP. In-memory, so it is per server
 * instance and resets on restart (fine for one instance; use Redis/DB if you scale out).
 */
@Component
public class LoginRateLimiter {

    private final LoginRateLimitProperties props;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> byEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> byIp = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(LoginRateLimitProperties props) {
        this(props, Clock.systemUTC());
    }

    LoginRateLimiter(LoginRateLimitProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    /** Throws 429 if this email or IP has used up its failures for the current window. */
    public void checkAllowed(String email, String ip) {
        long wait = Math.max(
                retryAfter(byEmail, email, props.maxFailuresPerEmail()),
                retryAfter(byIp, ip, props.maxFailuresPerIp()));
        if (wait > 0) {
            throw new TooManyRequestsException(wait);
        }
    }

    public void recordFailure(String email, String ip) {
        add(byEmail, email);
        add(byIp, ip);
    }

    /** A successful login clears the email's failures (not the IP's). */
    public void reset(String email) {
        byEmail.remove(email);
    }

    private long retryAfter(ConcurrentHashMap<String, Deque<Long>> map, String key, int max) {
        Deque<Long> d = map.get(key);
        if (d == null) {
            return 0;
        }
        synchronized (d) {
            prune(d);
            if (d.size() < max) {
                return 0;
            }
            long oldest = d.peekFirst();
            return Math.max(1, (oldest + props.window().toMillis() - now() + 999) / 1000);
        }
    }

    private void add(ConcurrentHashMap<String, Deque<Long>> map, String key) {
        Deque<Long> d = map.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (d) {
            prune(d);
            d.addLast(now());
        }
    }

    private void prune(Deque<Long> d) {
        long cutoff = now() - props.window().toMillis();
        while (!d.isEmpty() && d.peekFirst() <= cutoff) {
            d.pollFirst();
        }
    }

    private long now() {
        return clock.millis();
    }

    /** Drops keys whose failures have all expired, so the maps cannot grow forever. */
    @Scheduled(fixedRate = 600_000)
    void cleanup() {
        for (var map : java.util.List.of(byEmail, byIp)) {
            map.forEach((k, d) -> {
                synchronized (d) {
                    prune(d);
                    if (d.isEmpty()) {
                        map.remove(k, d);
                    }
                }
            });
        }
    }
}
