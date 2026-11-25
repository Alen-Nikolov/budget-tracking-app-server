package com.budgettracker.verification;

/** Published inside the transaction; the email is only sent after that transaction commits. */
public record VerificationEmailRequested(String toEmail, String link) {
}
