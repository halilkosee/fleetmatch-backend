package com.fleetmatch.auth.service;

public interface EmailService {

    void sendOtp(String email, String code, String purpose);
}
