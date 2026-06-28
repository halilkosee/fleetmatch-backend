package com.fleetmatch.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "fleetmatch.mail",
        name = "provider",
        havingValue = "smtp"
)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${fleetmatch.mail.from:no-reply@easyfleetmatch.com}")
    private String from;

    @Override
    public void sendOtp(String email, String code, String purpose) {
        sendEmail(
                email,
                "Your EasyFleetMatch verification code",
                "Your verification code for " + purpose + " is: " + code
        );
    }

    @Override
    public void sendEmail(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
