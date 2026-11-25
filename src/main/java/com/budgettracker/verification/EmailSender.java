package com.budgettracker.verification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends via SMTP when a mail server is configured (spring.mail.host).
 * Otherwise (local development) it just logs the link so you can open it from the console.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendVerification(String to, String link) throws Exception {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("No mail server configured. Verification link for {}: {}", to, link);
            return;
        }
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject("Verify your email address");
        // Plain-text part first, HTML second: mail apps show the best one they support.
        helper.setText(plainText(link), html(link));
        sender.send(message);
    }

    static String plainText(String link) {
        return "Welcome to Budget Tracker!\n\nPlease verify your email address by opening this link:\n\n" + link
                + "\n\nThe link is valid for 24 hours. If you did not create an account, ignore this email.";
    }

    static String html(String link) {
        String href = HtmlUtils.htmlEscape(link);
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#222">
                  <h2 style="margin:0 0 16px">Welcome to Budget Tracker!</h2>
                  <p style="line-height:1.5">Please confirm your email address to finish creating your account.</p>
                  <table role="presentation" cellspacing="0" cellpadding="0" style="margin:24px 0">
                    <tr><td style="background:#2563eb;border-radius:6px">
                      <a href="%s" style="display:inline-block;padding:12px 28px;color:#ffffff;font-weight:bold;text-decoration:none;font-size:16px">Verify email address</a>
                    </td></tr>
                  </table>
                  <p style="font-size:13px;color:#555;line-height:1.5">The link is valid for 24 hours.
                  If you did not create an account, you can ignore this email.</p>
                  <p style="font-size:12px;color:#888;line-height:1.5">Button not working? Copy this address into your browser:<br>%s</p>
                </div>
                """.formatted(href, href);
    }
}
