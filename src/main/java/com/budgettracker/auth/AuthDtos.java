package com.budgettracker.auth;

import com.budgettracker.user.Role;
import com.budgettracker.verification.ClientType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
            @NotBlank String confirmPassword,
            ClientType client) { // optional: WEB (default) or ANDROID, decides where the email link redirects

        @AssertTrue(message = "passwords do not match")
        public boolean isPasswordConfirmed() {
            return password != null && password.equals(confirmPassword);
        }
    }

    public record RegistrationResponse(String message, String email) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, long accessTokenExpiresInSeconds, String refreshToken,
                               long userId, String email, Role role) {
    }
}
