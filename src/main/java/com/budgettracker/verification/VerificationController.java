package com.budgettracker.verification;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerificationController {

    public record ResendRequest(@NotBlank @Email String email, ClientType client) {
    }

    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    /** The link in the email. Opened in a browser, so it answers with a redirect to the right client. */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        String url = service.redirectUrl(service.verify(token));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(@Valid @RequestBody ResendRequest req) {
        service.resend(req.email(), req.client() != null ? req.client() : ClientType.WEB);
    }
}
