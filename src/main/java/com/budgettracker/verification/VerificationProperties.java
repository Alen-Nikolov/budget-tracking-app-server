package com.budgettracker.verification;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(Duration expiration, Duration resendCooldown, String publicBaseUrl,
                                     String webRedirectUrl, String androidRedirectUrl) {
}
