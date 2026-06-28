package com.fleetmatch.auth.service;

public interface EmailService {

    void sendOtp(String email, String code, String purpose);

    void sendEmail(String email, String subject, String body);
}
