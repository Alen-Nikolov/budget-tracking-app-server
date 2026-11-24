package com.budgettracker.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.login-rate-limit")
public record LoginRateLimitProperties(int maxFailuresPerEmail, int maxFailuresPerIp, Duration window) {
}
