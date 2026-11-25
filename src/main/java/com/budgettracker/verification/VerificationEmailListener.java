package com.budgettracker.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VerificationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailListener.class);

    private final EmailSender sender;

    public VerificationEmailListener(EmailSender sender) {
        this.sender = sender;
    }

    /** After commit + off the request thread: a slow or failing mail server never breaks registration. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(VerificationEmailRequested event) {
        try {
            sender.sendVerification(event.toEmail(), event.link());
        } catch (Exception e) {
            log.error("Could not send verification email to {} (the user can request a new one)", event.toEmail(), e);
        }
    }
}
