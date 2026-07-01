package com.Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends the registration confirmation email asynchronously so the
     * register API does not block waiting on SMTP I/O.
     */
    @Async("taskExecutor")
    public void sendRegistrationEmail(String toEmail, String username) {
        if (!mailEnabled) {
            logger.info("Mail disabled. Skipping registration email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Welcome to our platform!");
            message.setText("Hi " + username + ",\n\nYour account has been registered successfully.\n\nThanks!");
            mailSender.send(message);
            logger.info("Registration email sent to {}", toEmail);
        } catch (Exception ex) {
            // Email failure must never break registration; just log it.
            logger.error("Failed to send registration email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
